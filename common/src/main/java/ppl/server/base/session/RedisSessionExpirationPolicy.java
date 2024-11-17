package ppl.server.base.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.Session;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class RedisSessionExpirationPolicy {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionExpirationPolicy.class);

    private static final String SESSION_EXPIRES_PREFIX = "expires:";

    private final RedisOperations<Object, Object> redis;

    private final Function<Long, String> lookupExpirationKey;

    private final Function<String, String> lookupSessionKey;

    RedisSessionExpirationPolicy(RedisOperations<Object, Object> sessionRedisOperations,
                                 Function<Long, String> lookupExpirationKey, Function<String, String> lookupSessionKey) {
        super();
        this.redis = sessionRedisOperations;
        this.lookupExpirationKey = lookupExpirationKey;
        this.lookupSessionKey = lookupSessionKey;
    }

    void onDelete(Session session) {
        long toExpire = roundUpToNextMinute(expiresInMillis(session));
        String expireKey = getExpirationKey(toExpire);
        String entryToRemove = SESSION_EXPIRES_PREFIX + session.getId();
        this.redis.boundSetOps(expireKey).remove(entryToRemove);
    }

    void onExpirationUpdated(Long originalExpirationTimeInMilli, Session session) {
        String keyToExpire = SESSION_EXPIRES_PREFIX + session.getId();
        long toExpire = roundUpToNextMinute(expiresInMillis(session));

        if (originalExpirationTimeInMilli != null) {
            long originalRoundedUp = roundUpToNextMinute(originalExpirationTimeInMilli);
            if (toExpire != originalRoundedUp) {
                String expireKey = getExpirationKey(originalRoundedUp);
                this.redis.boundSetOps(expireKey).remove(keyToExpire);
            }
        }

        long sessionExpireInSeconds = session.getMaxInactiveInterval().getSeconds();
        String sessionKey = getSessionKey(keyToExpire);

        if (sessionExpireInSeconds < 0) {
            this.redis.boundValueOps(sessionKey).append("");
            this.redis.boundValueOps(sessionKey).persist();
            this.redis.boundHashOps(getSessionKey(session.getId())).persist();
            return;
        }

        String expireKey = getExpirationKey(toExpire);
        BoundSetOperations<Object, Object> expireOperations = this.redis.boundSetOps(expireKey);
        expireOperations.add(keyToExpire);

        long fiveMinutesAfterExpires = sessionExpireInSeconds + TimeUnit.MINUTES.toSeconds(5);

        expireOperations.expire(fiveMinutesAfterExpires, TimeUnit.SECONDS);
        if (sessionExpireInSeconds == 0) {
            this.redis.delete(sessionKey);
        }
        else {
            this.redis.boundValueOps(sessionKey).append("");
            this.redis.boundValueOps(sessionKey).expire(sessionExpireInSeconds, TimeUnit.SECONDS);
        }
        this.redis.boundHashOps(getSessionKey(session.getId())).expire(fiveMinutesAfterExpires, TimeUnit.SECONDS);
    }

    String getExpirationKey(long expires) {
        return this.lookupExpirationKey.apply(expires);
    }

    String getSessionKey(String sessionId) {
        return this.lookupSessionKey.apply(sessionId);
    }

    void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        long prevMin = roundDownMinute(now);

        if (log.isDebugEnabled()) {
            log.debug("Cleaning up sessions expiring at " + new Date(prevMin));
        }

        String expirationKey = getExpirationKey(prevMin);
        Set<Object> sessionsToExpire = this.redis.boundSetOps(expirationKey).members();
        sessionsToExpire = sessionsToExpire == null ? Collections.emptySet() : sessionsToExpire;
        this.redis.delete(expirationKey);
        for (Object session : sessionsToExpire) {
            String sessionKey = getSessionKey((String) session);
            touch(sessionKey);
        }
    }

    /**
     * By trying to access the session we only trigger a deletion if it the TTL is
     * expired. This is done to handle
     * https://github.com/spring-projects/spring-session/issues/93
     * @param key the key
     */
    private void touch(String key) {
        this.redis.hasKey(key);
    }

    static long expiresInMillis(Session session) {
        int maxInactiveInSeconds = (int) session.getMaxInactiveInterval().getSeconds();
        long lastAccessedTimeInMillis = session.getLastAccessedTime().toEpochMilli();
        return lastAccessedTimeInMillis + TimeUnit.SECONDS.toMillis(maxInactiveInSeconds);
    }

    static long roundUpToNextMinute(long timeInMs) {

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timeInMs);
        date.add(Calendar.MINUTE, 1);
        date.clear(Calendar.SECOND);
        date.clear(Calendar.MILLISECOND);
        return date.getTimeInMillis();
    }

    static long roundDownMinute(long timeInMs) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timeInMs);
        date.clear(Calendar.SECOND);
        date.clear(Calendar.MILLISECOND);
        return date.getTimeInMillis();
    }

}