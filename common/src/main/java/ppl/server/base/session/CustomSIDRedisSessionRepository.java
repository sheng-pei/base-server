package ppl.server.base.session;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.*;
import ppl.common.utils.string.Strings;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class CustomSIDRedisSessionRepository implements SessionRepository<CustomSIDRedisSessionRepository.RedisSession> {

    public static final String DEFAULT_KEY_NAMESPACE = "spring:session";

    private final RedisOperations<String, Object> sessionRedisOperations;

    private Duration defaultMaxInactiveInterval = Duration.ofSeconds(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);

    private String keyNamespace = DEFAULT_KEY_NAMESPACE + ":";

    private FlushMode flushMode = FlushMode.ON_SAVE;

    private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

    private SessionIdGenerator sessionIdGenerator = UuidSessionIdGenerator.getInstance();

    public CustomSIDRedisSessionRepository(RedisOperations<String, Object> sessionRedisOperations) {
        Objects.requireNonNull(sessionRedisOperations, "sessionRedisOperations mut not be null");
        this.sessionRedisOperations = sessionRedisOperations;
    }

    public void setDefaultMaxInactiveInterval(Duration defaultMaxInactiveInterval) {
        Objects.requireNonNull(defaultMaxInactiveInterval, "defaultMaxInactiveInterval must not be null");
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    @Deprecated
    public void setKeyNamespace(String keyNamespace) {
        if (Strings.isBlank(keyNamespace)) {
            throw new IllegalArgumentException("keyNamespace must not be empty");
        }
        this.keyNamespace = keyNamespace;
    }

    public void setRedisKeyNamespace(String namespace) {
        if (Strings.isBlank(namespace)) {
            throw new IllegalArgumentException("namespace must not be empty");
        }
        this.keyNamespace = namespace.trim() + ":";
    }

    public void setFlushMode(FlushMode flushMode) {
        Objects.requireNonNull(flushMode, "flushMode must not be null");
        this.flushMode = flushMode;
    }

    public void setSaveMode(SaveMode saveMode) {
        Objects.requireNonNull(saveMode, "saveMode must not be null");
        this.saveMode = saveMode;
    }

    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator cannot be null");
        this.sessionIdGenerator = sessionIdGenerator;
    }

    @Override
    public RedisSession createSession() {
        MapSession cached = new MapSession(sessionIdGenerator.generate());
        cached.setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        RedisSession session = new RedisSession(cached, true);
        session.flushIfRequired();
        return session;
    }

    @Override
    public void save(RedisSession session) {
        if (!session.isNew) {
            String key = getSessionKey(session.hasChangedSessionId() ? session.originalSessionId : session.getId());
            Boolean sessionExists = this.sessionRedisOperations.hasKey(key);
            if (sessionExists == null || !sessionExists) {
                throw new IllegalStateException("Session was invalidated");
            }
        }
        session.save();
    }

    @Override
    public RedisSession findById(String sessionId) {
        String key = getSessionKey(sessionId);
        Map<String, Object> entries = this.sessionRedisOperations.<String, Object>opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        MapSession session = new RedisSessionMapper(sessionId).apply(entries);
        if (session.isExpired()) {
            deleteById(sessionId);
            return null;
        }
        return new RedisSession(session, false);
    }

    @Override
    public void deleteById(String sessionId) {
        String key = getSessionKey(sessionId);
        this.sessionRedisOperations.delete(key);
    }

    public RedisOperations<String, Object> getSessionRedisOperations() {
        return this.sessionRedisOperations;
    }

    private String getSessionKey(String sessionId) {
        return this.keyNamespace + "sessions:" + sessionId;
    }

    private static String getAttributeKey(String attributeName) {
        return RedisSessionMapper.ATTRIBUTE_PREFIX + attributeName;
    }

    final class RedisSession implements Session {

        private final MapSession cached;

        private final Map<String, Object> delta = new HashMap<>();

        private boolean isNew;

        private String originalSessionId;

        RedisSession(MapSession cached, boolean isNew) {
            this.cached = cached;
            this.isNew = isNew;
            this.originalSessionId = cached.getId();
            if (this.isNew) {
                this.delta.put(RedisSessionMapper.CREATION_TIME_KEY, cached.getCreationTime().toEpochMilli());
                this.delta.put(RedisSessionMapper.MAX_INACTIVE_INTERVAL_KEY,
                        (int) cached.getMaxInactiveInterval().getSeconds());
                this.delta.put(RedisSessionMapper.LAST_ACCESSED_TIME_KEY, cached.getLastAccessedTime().toEpochMilli());
            }
            if (this.isNew || (CustomSIDRedisSessionRepository.this.saveMode == SaveMode.ALWAYS)) {
                getAttributeNames().forEach((attributeName) -> this.delta.put(getAttributeKey(attributeName),
                        cached.getAttribute(attributeName)));
            }
        }

        @Override
        public String getId() {
            return this.cached.getId();
        }

        @Override
        public String changeSessionId() {
            String id = sessionIdGenerator.generate();
            this.cached.setId(id);
            return id;
        }

        @Override
        public <T> T getAttribute(String attributeName) {
            T attributeValue = this.cached.getAttribute(attributeName);
            if (attributeValue != null && CustomSIDRedisSessionRepository.this.saveMode.equals(SaveMode.ON_GET_ATTRIBUTE)) {
                this.delta.put(getAttributeKey(attributeName), attributeValue);
            }
            return attributeValue;
        }

        @Override
        public Set<String> getAttributeNames() {
            return this.cached.getAttributeNames();
        }

        @Override
        public void setAttribute(String attributeName, Object attributeValue) {
            this.cached.setAttribute(attributeName, attributeValue);
            this.delta.put(getAttributeKey(attributeName), attributeValue);
            flushIfRequired();
        }

        @Override
        public void removeAttribute(String attributeName) {
            setAttribute(attributeName, null);
        }

        @Override
        public Instant getCreationTime() {
            return this.cached.getCreationTime();
        }

        @Override
        public void setLastAccessedTime(Instant lastAccessedTime) {
            this.cached.setLastAccessedTime(lastAccessedTime);
            this.delta.put(RedisSessionMapper.LAST_ACCESSED_TIME_KEY, getLastAccessedTime().toEpochMilli());
            flushIfRequired();
        }

        @Override
        public Instant getLastAccessedTime() {
            return this.cached.getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(Duration interval) {
            this.cached.setMaxInactiveInterval(interval);
            this.delta.put(RedisSessionMapper.MAX_INACTIVE_INTERVAL_KEY, (int) getMaxInactiveInterval().getSeconds());
            flushIfRequired();
        }

        @Override
        public Duration getMaxInactiveInterval() {
            return this.cached.getMaxInactiveInterval();
        }

        @Override
        public boolean isExpired() {
            return this.cached.isExpired();
        }

        private void flushIfRequired() {
            if (CustomSIDRedisSessionRepository.this.flushMode == FlushMode.IMMEDIATE) {
                save();
            }
        }

        private boolean hasChangedSessionId() {
            return !getId().equals(this.originalSessionId);
        }

        private void save() {
            saveChangeSessionId();
            saveDelta();
            if (this.isNew) {
                this.isNew = false;
            }
        }

        private void saveChangeSessionId() {
            if (hasChangedSessionId()) {
                if (!this.isNew) {
                    String originalSessionIdKey = getSessionKey(this.originalSessionId);
                    String sessionIdKey = getSessionKey(getId());
                    CustomSIDRedisSessionRepository.this.sessionRedisOperations.rename(originalSessionIdKey, sessionIdKey);
                }
                this.originalSessionId = getId();
            }
        }

        private void saveDelta() {
            if (this.delta.isEmpty()) {
                return;
            }
            String key = getSessionKey(getId());
            CustomSIDRedisSessionRepository.this.sessionRedisOperations.opsForHash().putAll(key, new HashMap<>(this.delta));
            CustomSIDRedisSessionRepository.this.sessionRedisOperations.expireAt(key,
                    Date.from(Instant.ofEpochMilli(getLastAccessedTime().toEpochMilli())
                            .plusSeconds(getMaxInactiveInterval().getSeconds())));
            this.delta.clear();
        }

    }

}
