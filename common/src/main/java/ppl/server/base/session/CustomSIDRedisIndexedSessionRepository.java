package ppl.server.base.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.util.ByteUtils;
import org.springframework.session.*;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import ppl.common.utils.string.Strings;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * <p>
 * One problem with relying on Redis expiration exclusively is that Redis makes no
 * guarantee of when the expired event will be fired if the key has not been accessed.
 * Specifically the background task that Redis uses to clean up expired keys is a low
 * priority task and may not trigger the key expiration. For additional details see
 * <a href="https://redis.io/topics/notifications">Timing of expired events</a> section in
 * the Redis documentation.
 * </p>
 *
 * <p>
 * To circumvent the fact that expired events are not guaranteed to happen we can ensure
 * that each key is accessed when it is expected to expire. This means that if the TTL is
 * expired on the key, Redis will remove the key and fire the expired event when we try to
 * access the key.
 * </p>
 *
 * <p>
 * For this reason, each session expiration is also tracked to the nearest minute. This
 * allows a background task to access the potentially expired sessions to ensure that
 * Redis expired events are fired in a more deterministic fashion. For example:
 * </p>
 *
 * <pre>
 * SADD spring:session:expirations:1439245080000 expires:33fdd1b6-b496-4b33-9f7d-df96679d32fe
 * EXPIRE spring:session:expirations1439245080000 2100
 * </pre>
 *
 * <p>
 * The background task will then use these mappings to explicitly request each session
 * expires key. By accessing the key, rather than deleting it, we ensure that Redis
 * deletes the key for us only if the TTL is expired.
 * </p>
 * <p>
 * <b>NOTE</b>: We do not explicitly delete the keys since in some instances there may be
 * a race condition that incorrectly identifies a key as expired when it is not. Short of
 * using distributed locks (which would kill our performance) there is no way to ensure
 * the consistency of the expiration mapping. By simply accessing the key, we ensure that
 * the key is only removed if the TTL on that key is expired.
 * </p>
 */
public class CustomSIDRedisIndexedSessionRepository
        implements FindByIndexNameSessionRepository<CustomSIDRedisIndexedSessionRepository.RedisSession>, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CustomSIDRedisIndexedSessionRepository.class);

    private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

    public static final int DEFAULT_DATABASE = 0;

    public static final String DEFAULT_NAMESPACE = "spring:session";

    private int database = DEFAULT_DATABASE;

    private String namespace = DEFAULT_NAMESPACE + ":";

    private String sessionCreatedChannelPrefix;

    private byte[] sessionCreatedChannelPrefixBytes;

    private String sessionDeletedChannel;

    private byte[] sessionDeletedChannelBytes;

    private String sessionExpiredChannel;

    private byte[] sessionExpiredChannelBytes;

    private String expiredKeyPrefix;

    private byte[] expiredKeyPrefixBytes;

    private final RedisOperations<Object, Object> sessionRedisOperations;

    private final RedisSessionExpirationPolicy expirationPolicy;

    private ApplicationEventPublisher eventPublisher = (event) -> {
    };

    private Integer defaultMaxInactiveInterval;

    private IndexResolver<Session> indexResolver = new DelegatingIndexResolver<>(new PrincipalNameIndexResolver<>());

    private RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer();

    private FlushMode flushMode = FlushMode.ON_SAVE;

    private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

    private SessionIdGenerator sessionIdGenerator = UuidSessionIdGenerator.getInstance();

    public CustomSIDRedisIndexedSessionRepository(RedisOperations<Object, Object> sessionRedisOperations) {
        Objects.requireNonNull(sessionRedisOperations, "sessionRedisOperations cannot be null");
        this.sessionRedisOperations = sessionRedisOperations;
        this.expirationPolicy = new RedisSessionExpirationPolicy(sessionRedisOperations, this::getExpirationsKey,
                this::getSessionKey);
        configureSessionChannels();
    }

    /**
     * Sets the {@link ApplicationEventPublisher} that is used to publish
     * {@link SessionDestroyedEvent}. The default is to not publish a
     * {@link SessionDestroyedEvent}.
     * @param applicationEventPublisher the {@link ApplicationEventPublisher} that is used
     * to publish {@link SessionDestroyedEvent}. Cannot be null.
     */
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        Objects.requireNonNull(applicationEventPublisher, "applicationEventPublisher cannot be null");
        this.eventPublisher = applicationEventPublisher;
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    public void setIndexResolver(IndexResolver<Session> indexResolver) {
        Objects.requireNonNull(indexResolver, "indexResolver cannot be null");
        this.indexResolver = indexResolver;
    }

    public void setDefaultSerializer(RedisSerializer<Object> defaultSerializer) {
        Objects.requireNonNull(defaultSerializer, "defaultSerializer cannot be null");
        this.defaultSerializer = defaultSerializer;
    }

    public void setFlushMode(FlushMode flushMode) {
        Objects.requireNonNull(flushMode, "flushMode cannot be null");
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

    public void setDatabase(int database) {
        this.database = database;
        configureSessionChannels();
    }

    private void configureSessionChannels() {
        this.sessionCreatedChannelPrefix = this.namespace + "event:" + this.database + ":created:";
        this.sessionCreatedChannelPrefixBytes = this.sessionCreatedChannelPrefix.getBytes();
        this.sessionDeletedChannel = "__keyevent@" + this.database + "__:del";
        this.sessionDeletedChannelBytes = this.sessionDeletedChannel.getBytes();
        this.sessionExpiredChannel = "__keyevent@" + this.database + "__:expired";
        this.sessionExpiredChannelBytes = this.sessionExpiredChannel.getBytes();
        this.expiredKeyPrefix = this.namespace + "sessions:expires:";
        this.expiredKeyPrefixBytes = this.expiredKeyPrefix.getBytes();
    }

    public RedisOperations<Object, Object> getSessionRedisOperations() {
        return this.sessionRedisOperations;
    }

    @Override
    public void save(RedisSession session) {
        session.save();
        if (session.isNew) {
            String sessionCreatedKey = getSessionCreatedChannel(session.getId());
            this.sessionRedisOperations.convertAndSend(sessionCreatedKey, session.delta);
            session.isNew = false;
        }
    }

    public void cleanupExpiredSessions() {
        this.expirationPolicy.cleanExpiredSessions();
    }

    @Override
    public RedisSession findById(String id) {
        return getSession(id, false);
    }

    @Override
    public Map<String, RedisSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
        if (!FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME.equals(indexName)) {
            return Collections.emptyMap();
        }
        String principalKey = getPrincipalKey(indexValue);
        Set<Object> sessionIds = this.sessionRedisOperations.boundSetOps(principalKey).members();
        Map<String, RedisSession> sessions = new HashMap<>(sessionIds.size());
        for (Object id : sessionIds) {
            RedisSession session = findById((String) id);
            if (session != null) {
                sessions.put(session.getId(), session);
            }
        }
        return sessions;
    }

    private RedisSession getSession(String id, boolean allowExpired) {
        Map<Object, Object> entries = getSessionBoundHashOperations(id).entries();
        if (entries.isEmpty()) {
            return null;
        }
        MapSession loaded = loadSession(id, entries);
        if (!allowExpired && loaded.isExpired()) {
            return null;
        }
        RedisSession result = new RedisSession(loaded, false);
        result.originalLastAccessTime = loaded.getLastAccessedTime();
        return result;
    }

    private MapSession loadSession(String id, Map<Object, Object> entries) {
        MapSession loaded = new MapSession(id);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String key = (String) entry.getKey();
            if (RedisSessionMapper.CREATION_TIME_KEY.equals(key)) {
                loaded.setCreationTime(Instant.ofEpochMilli((long) entry.getValue()));
            }
            else if (RedisSessionMapper.MAX_INACTIVE_INTERVAL_KEY.equals(key)) {
                loaded.setMaxInactiveInterval(Duration.ofSeconds((int) entry.getValue()));
            }
            else if (RedisSessionMapper.LAST_ACCESSED_TIME_KEY.equals(key)) {
                loaded.setLastAccessedTime(Instant.ofEpochMilli((long) entry.getValue()));
            }
            else if (key.startsWith(RedisSessionMapper.ATTRIBUTE_PREFIX)) {
                loaded.setAttribute(key.substring(RedisSessionMapper.ATTRIBUTE_PREFIX.length()), entry.getValue());
            }
        }
        return loaded;
    }

    @Override
    public void deleteById(String sessionId) {
        RedisSession session = getSession(sessionId, true);
        if (session == null) {
            return;
        }

        cleanupPrincipalIndex(session);
        this.expirationPolicy.onDelete(session);

        String expireKey = getExpiredKey(session.getId());
        this.sessionRedisOperations.delete(expireKey);

        session.setMaxInactiveInterval(Duration.ZERO);
        save(session);
    }

    @Override
    public RedisSession createSession() {
        MapSession cached = new MapSession(sessionIdGenerator.generate());
        if (this.defaultMaxInactiveInterval != null) {
            cached.setMaxInactiveInterval(Duration.ofSeconds(this.defaultMaxInactiveInterval));
        }
        RedisSession session = new RedisSession(cached, true);
        session.flushImmediateIfNecessary();
        return session;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] messageChannel = message.getChannel();

        if (ByteUtils.startsWith(messageChannel, this.sessionCreatedChannelPrefixBytes)) {
            // TODO: is this thread safe?
            @SuppressWarnings("unchecked")
            Map<Object, Object> loaded = (Map<Object, Object>) this.defaultSerializer.deserialize(message.getBody());
            handleCreated(loaded, new String(messageChannel));
            return;
        }

        byte[] messageBody = message.getBody();

        if (!ByteUtils.startsWith(messageBody, this.expiredKeyPrefixBytes)) {
            return;
        }

        boolean isDeleted = Arrays.equals(messageChannel, this.sessionDeletedChannelBytes);
        if (isDeleted || Arrays.equals(messageChannel, this.sessionExpiredChannelBytes)) {
            String body = new String(messageBody);
            int beginIndex = body.lastIndexOf(":") + 1;
            int endIndex = body.length();
            String sessionId = body.substring(beginIndex, endIndex);

            RedisSession session = getSession(sessionId, true);

            if (session == null) {
                log.warn("Unable to publish SessionDestroyedEvent for session " + sessionId);
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Publishing SessionDestroyedEvent for session " + sessionId);
            }

            cleanupPrincipalIndex(session);

            if (isDeleted) {
                handleDeleted(session);
            }
            else {
                handleExpired(session);
            }
        }
    }

    private void cleanupPrincipalIndex(RedisSession session) {
        String sessionId = session.getId();
        Map<String, String> indexes = CustomSIDRedisIndexedSessionRepository.this.indexResolver.resolveIndexesFor(session);
        String principal = indexes.get(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        if (principal != null) {
            this.sessionRedisOperations.boundSetOps(getPrincipalKey(principal)).remove(sessionId);
        }
    }

    private void handleCreated(Map<Object, Object> loaded, String channel) {
        String id = channel.substring(channel.lastIndexOf(":") + 1);
        Session session = loadSession(id, loaded);
        publishEvent(new SessionCreatedEvent(this, session));
    }

    private void handleDeleted(RedisSession session) {
        publishEvent(new SessionDeletedEvent(this, session));
    }

    private void handleExpired(RedisSession session) {
        publishEvent(new SessionExpiredEvent(this, session));
    }

    private void publishEvent(ApplicationEvent event) {
        try {
            this.eventPublisher.publishEvent(event);
        }
        catch (Throwable ex) {
            log.error("Error publishing " + event + ".", ex);
        }
    }

    public void setRedisKeyNamespace(String namespace) {
        if (Strings.isBlank(namespace)) {
            throw new IllegalArgumentException("namespace must not be empty");
        }
        this.namespace = namespace.trim() + ":";
        configureSessionChannels();
    }

    String getSessionKey(String sessionId) {
        return this.namespace + "sessions:" + sessionId;
    }

    String getPrincipalKey(String principalName) {
        return this.namespace + "index:" + FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME + ":"
                + principalName;
    }

    String getExpirationsKey(long expiration) {
        return this.namespace + "expirations:" + expiration;
    }

    private String getExpiredKey(String sessionId) {
        return getExpiredKeyPrefix() + sessionId;
    }

    private String getSessionCreatedChannel(String sessionId) {
        return getSessionCreatedChannelPrefix() + sessionId;
    }

    private String getExpiredKeyPrefix() {
        return this.expiredKeyPrefix;
    }

    public String getSessionCreatedChannelPrefix() {
        return this.sessionCreatedChannelPrefix;
    }

    public String getSessionDeletedChannel() {
        return this.sessionDeletedChannel;
    }

    public String getSessionExpiredChannel() {
        return this.sessionExpiredChannel;
    }

    private BoundHashOperations<Object, Object, Object> getSessionBoundHashOperations(String sessionId) {
        String key = getSessionKey(sessionId);
        return this.sessionRedisOperations.boundHashOps(key);
    }

    static String getSessionAttrNameKey(String attributeName) {
        return RedisSessionMapper.ATTRIBUTE_PREFIX + attributeName;
    }

    final class RedisSession implements Session {

        private final MapSession cached;

        private Instant originalLastAccessTime;

        private Map<String, Object> delta = new HashMap<>();

        private boolean isNew;

        private String originalPrincipalName;

        private String originalSessionId;

        RedisSession(MapSession cached, boolean isNew) {
            this.cached = cached;
            this.isNew = isNew;
            this.originalSessionId = cached.getId();
            Map<String, String> indexes = CustomSIDRedisIndexedSessionRepository.this.indexResolver.resolveIndexesFor(this);
            this.originalPrincipalName = indexes.get(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
            if (this.isNew) {
                this.delta.put(RedisSessionMapper.CREATION_TIME_KEY, cached.getCreationTime().toEpochMilli());
                this.delta.put(RedisSessionMapper.MAX_INACTIVE_INTERVAL_KEY,
                        (int) cached.getMaxInactiveInterval().getSeconds());
                this.delta.put(RedisSessionMapper.LAST_ACCESSED_TIME_KEY, cached.getLastAccessedTime().toEpochMilli());
            }
            if (this.isNew || (CustomSIDRedisIndexedSessionRepository.this.saveMode == SaveMode.ALWAYS)) {
                getAttributeNames().forEach((attributeName) -> this.delta.put(getSessionAttrNameKey(attributeName),
                        cached.getAttribute(attributeName)));
            }
        }

        @Override
        public void setLastAccessedTime(Instant lastAccessedTime) {
            this.cached.setLastAccessedTime(lastAccessedTime);
            this.delta.put(RedisSessionMapper.LAST_ACCESSED_TIME_KEY, getLastAccessedTime().toEpochMilli());
            flushImmediateIfNecessary();
        }

        @Override
        public boolean isExpired() {
            return this.cached.isExpired();
        }

        @Override
        public Instant getCreationTime() {
            return this.cached.getCreationTime();
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
        public Instant getLastAccessedTime() {
            return this.cached.getLastAccessedTime();
        }

        @Override
        public void setMaxInactiveInterval(Duration interval) {
            this.cached.setMaxInactiveInterval(interval);
            this.delta.put(RedisSessionMapper.MAX_INACTIVE_INTERVAL_KEY, (int) getMaxInactiveInterval().getSeconds());
            flushImmediateIfNecessary();
        }

        @Override
        public Duration getMaxInactiveInterval() {
            return this.cached.getMaxInactiveInterval();
        }

        @Override
        public <T> T getAttribute(String attributeName) {
            T attributeValue = this.cached.getAttribute(attributeName);
            if (attributeValue != null
                    && CustomSIDRedisIndexedSessionRepository.this.saveMode.equals(SaveMode.ON_GET_ATTRIBUTE)) {
                this.delta.put(getSessionAttrNameKey(attributeName), attributeValue);
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
            this.delta.put(getSessionAttrNameKey(attributeName), attributeValue);
            flushImmediateIfNecessary();
        }

        @Override
        public void removeAttribute(String attributeName) {
            this.cached.removeAttribute(attributeName);
            this.delta.put(getSessionAttrNameKey(attributeName), null);
            flushImmediateIfNecessary();
        }

        private void flushImmediateIfNecessary() {
            if (CustomSIDRedisIndexedSessionRepository.this.flushMode == FlushMode.IMMEDIATE) {
                save();
            }
        }

        private void save() {
            saveChangeSessionId();
            saveDelta();
        }

        private void saveDelta() {
            if (this.delta.isEmpty()) {
                return;
            }
            String sessionId = getId();
            getSessionBoundHashOperations(sessionId).putAll(this.delta);
            String principalSessionKey = getSessionAttrNameKey(
                    FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
            String securityPrincipalSessionKey = getSessionAttrNameKey(SPRING_SECURITY_CONTEXT);
            if (this.delta.containsKey(principalSessionKey) || this.delta.containsKey(securityPrincipalSessionKey)) {
                if (this.originalPrincipalName != null) {
                    String originalPrincipalRedisKey = getPrincipalKey(this.originalPrincipalName);
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.boundSetOps(originalPrincipalRedisKey)
                            .remove(sessionId);
                }
                Map<String, String> indexes = CustomSIDRedisIndexedSessionRepository.this.indexResolver.resolveIndexesFor(this);
                String principal = indexes.get(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
                this.originalPrincipalName = principal;
                if (principal != null) {
                    String principalRedisKey = getPrincipalKey(principal);
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.boundSetOps(principalRedisKey)
                            .add(sessionId);
                }
            }

            this.delta = new HashMap<>(this.delta.size());

            Long originalExpiration = (this.originalLastAccessTime != null)
                    ? this.originalLastAccessTime.plus(getMaxInactiveInterval()).toEpochMilli() : null;
            CustomSIDRedisIndexedSessionRepository.this.expirationPolicy.onExpirationUpdated(originalExpiration, this);
        }

        private void saveChangeSessionId() {
            String sessionId = getId();
            if (sessionId.equals(this.originalSessionId)) {
                return;
            }
            if (!this.isNew) {
                String originalSessionIdKey = getSessionKey(this.originalSessionId);
                String sessionIdKey = getSessionKey(sessionId);
                try {
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.rename(originalSessionIdKey,
                            sessionIdKey);
                }
                catch (NonTransientDataAccessException ex) {
                    handleErrNoSuchKeyError(ex);
                }
                String originalExpiredKey = getExpiredKey(this.originalSessionId);
                String expiredKey = getExpiredKey(sessionId);
                try {
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.rename(originalExpiredKey, expiredKey);
                }
                catch (NonTransientDataAccessException ex) {
                    handleErrNoSuchKeyError(ex);
                }
                if (this.originalPrincipalName != null) {
                    String originalPrincipalRedisKey = getPrincipalKey(this.originalPrincipalName);
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.boundSetOps(originalPrincipalRedisKey)
                            .remove(this.originalSessionId);
                    CustomSIDRedisIndexedSessionRepository.this.sessionRedisOperations.boundSetOps(originalPrincipalRedisKey)
                            .add(sessionId);
                }
            }
            this.originalSessionId = sessionId;
        }

        private void handleErrNoSuchKeyError(NonTransientDataAccessException ex) {
            String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            String prefix = "ERR no such key";
            if (message == null || message.length() < prefix.length() || !message.regionMatches(true, 0, prefix, 0, prefix.length())) {
                throw ex;
            }
        }

    }

}
