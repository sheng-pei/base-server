package ppl.server.base.session;

import org.springframework.session.MapSession;
import ppl.common.utils.string.Strings;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

final class RedisSessionMapper implements Function<Map<String, Object>, MapSession> {

    static final String CREATION_TIME_KEY = "creationTime";

    static final String LAST_ACCESSED_TIME_KEY = "lastAccessedTime";

    static final String MAX_INACTIVE_INTERVAL_KEY = "maxInactiveInterval";

    static final String ATTRIBUTE_PREFIX = "sessionAttr:";

    private final String sessionId;

    RedisSessionMapper(String sessionId) {
        if (Strings.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionId must not be empty");
        }
        this.sessionId = sessionId;
    }

    @Override
    public MapSession apply(Map<String, Object> map) {
        if (map.isEmpty()) {
            throw new IllegalArgumentException("map must not be empty");
        }
        MapSession session = new MapSession(this.sessionId);
        Long creationTime = (Long) map.get(CREATION_TIME_KEY);
        if (creationTime == null) {
            handleMissingKey(CREATION_TIME_KEY);
        }
        session.setCreationTime(Instant.ofEpochMilli(creationTime));
        Long lastAccessedTime = (Long) map.get(LAST_ACCESSED_TIME_KEY);
        if (lastAccessedTime == null) {
            handleMissingKey(LAST_ACCESSED_TIME_KEY);
        }
        session.setLastAccessedTime(Instant.ofEpochMilli(lastAccessedTime));
        Integer maxInactiveInterval = (Integer) map.get(MAX_INACTIVE_INTERVAL_KEY);
        if (maxInactiveInterval == null) {
            handleMissingKey(MAX_INACTIVE_INTERVAL_KEY);
        }
        session.setMaxInactiveInterval(Duration.ofSeconds(maxInactiveInterval));
        map.forEach((name, value) -> {
            if (name.startsWith(ATTRIBUTE_PREFIX)) {
                session.setAttribute(name.substring(ATTRIBUTE_PREFIX.length()), value);
            }
        });
        return session;
    }

    private static void handleMissingKey(String key) {
        throw new IllegalStateException(key + " key must not be null");
    }

}