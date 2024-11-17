package ppl.server.base.session;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.session.*;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.SpringSessionRedisConnectionFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;
import ppl.common.utils.string.Strings;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class CustomRedisHttpSessionConfiguration extends SpringHttpSessionConfiguration
        implements BeanClassLoaderAware, EmbeddedValueResolverAware, ImportAware {

    static final String DEFAULT_CLEANUP_CRON = "0 * * * * *";

    private Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    private String redisNamespace = CustomSIDRedisIndexedSessionRepository.DEFAULT_NAMESPACE;

    private FlushMode flushMode = FlushMode.ON_SAVE;

    private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

    private String cleanupCron = DEFAULT_CLEANUP_CRON;

    private ConfigureRedisAction configureRedisAction = new ConfigureNotifyKeyspaceEventsAction();

    private RedisConnectionFactory redisConnectionFactory;

    private IndexResolver<Session> indexResolver;

    private RedisSerializer<Object> defaultRedisSerializer;

    private ApplicationEventPublisher applicationEventPublisher;

    private Executor redisTaskExecutor;

    private Executor redisSubscriptionExecutor;

    private List<SessionRepositoryCustomizer<CustomSIDRedisIndexedSessionRepository>> sessionRepositoryCustomizers;

    private ClassLoader classLoader;

    private StringValueResolver embeddedValueResolver;

    @Bean
    public CustomSIDRedisIndexedSessionRepository sessionRepository() {
        RedisTemplate<Object, Object> redisTemplate = createRedisTemplate();
        CustomSIDRedisIndexedSessionRepository sessionRepository = new CustomSIDRedisIndexedSessionRepository(redisTemplate);
        sessionRepository.setApplicationEventPublisher(this.applicationEventPublisher);
        if (this.indexResolver != null) {
            sessionRepository.setIndexResolver(this.indexResolver);
        }
        if (this.defaultRedisSerializer != null) {
            sessionRepository.setDefaultSerializer(this.defaultRedisSerializer);
        }
        sessionRepository.setDefaultMaxInactiveInterval(this.maxInactiveIntervalInSeconds);
        if (Strings.isNotBlank(this.redisNamespace)) {
            sessionRepository.setRedisKeyNamespace(this.redisNamespace);
        }
        sessionRepository.setFlushMode(this.flushMode);
        sessionRepository.setSaveMode(this.saveMode);
        int database = resolveDatabase();
        sessionRepository.setDatabase(database);
        this.sessionRepositoryCustomizers
                .forEach((sessionRepositoryCustomizer) -> sessionRepositoryCustomizer.customize(sessionRepository));
        return sessionRepository;
    }

    @Bean
    public RedisMessageListenerContainer springSessionRedisMessageListenerContainer(
            CustomSIDRedisIndexedSessionRepository sessionRepository) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(this.redisConnectionFactory);
        if (this.redisTaskExecutor != null) {
            container.setTaskExecutor(this.redisTaskExecutor);
        }
        if (this.redisSubscriptionExecutor != null) {
            container.setSubscriptionExecutor(this.redisSubscriptionExecutor);
        }
        container.addMessageListener(sessionRepository,
                Arrays.asList(new ChannelTopic(sessionRepository.getSessionDeletedChannel()),
                        new ChannelTopic(sessionRepository.getSessionExpiredChannel())));
        container.addMessageListener(sessionRepository,
                Collections.singletonList(new PatternTopic(sessionRepository.getSessionCreatedChannelPrefix() + "*")));
        return container;
    }

    @Bean
    public InitializingBean enableRedisKeyspaceNotificationsInitializer() {
        return new CustomRedisHttpSessionConfiguration.EnableRedisKeyspaceNotificationsInitializer(this.redisConnectionFactory, this.configureRedisAction);
    }

    public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public void setRedisNamespace(String namespace) {
        this.redisNamespace = namespace;
    }

    @Deprecated
    public void setRedisFlushMode(RedisFlushMode redisFlushMode) {
        Objects.requireNonNull(redisFlushMode, "redisFlushMode cannot be null");
        setFlushMode(redisFlushMode.getFlushMode());
    }

    public void setFlushMode(FlushMode flushMode) {
        Objects.requireNonNull(flushMode, "flushMode cannot be null");
        this.flushMode = flushMode;
    }

    public void setSaveMode(SaveMode saveMode) {
        this.saveMode = saveMode;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }

    @Autowired(required = false)
    public void setConfigureRedisAction(ConfigureRedisAction configureRedisAction) {
        this.configureRedisAction = configureRedisAction;
    }

    @Autowired
    public void setRedisConnectionFactory(
            @SpringSessionRedisConnectionFactory ObjectProvider<RedisConnectionFactory> springSessionRedisConnectionFactory,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactory) {
        RedisConnectionFactory redisConnectionFactoryToUse = springSessionRedisConnectionFactory.getIfAvailable();
        if (redisConnectionFactoryToUse == null) {
            redisConnectionFactoryToUse = redisConnectionFactory.getObject();
        }
        this.redisConnectionFactory = redisConnectionFactoryToUse;
    }

    @Autowired(required = false)
    @Qualifier("springSessionDefaultRedisSerializer")
    public void setDefaultRedisSerializer(RedisSerializer<Object> defaultRedisSerializer) {
        this.defaultRedisSerializer = defaultRedisSerializer;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Autowired(required = false)
    public void setIndexResolver(IndexResolver<Session> indexResolver) {
        this.indexResolver = indexResolver;
    }

    @Autowired(required = false)
    @Qualifier("springSessionRedisTaskExecutor")
    public void setRedisTaskExecutor(Executor redisTaskExecutor) {
        this.redisTaskExecutor = redisTaskExecutor;
    }

    @Autowired(required = false)
    @Qualifier("springSessionRedisSubscriptionExecutor")
    public void setRedisSubscriptionExecutor(Executor redisSubscriptionExecutor) {
        this.redisSubscriptionExecutor = redisSubscriptionExecutor;
    }

    @Autowired(required = false)
    public void setSessionRepositoryCustomizer(
            ObjectProvider<SessionRepositoryCustomizer<CustomSIDRedisIndexedSessionRepository>> sessionRepositoryCustomizers) {
        this.sessionRepositoryCustomizers = sessionRepositoryCustomizers.orderedStream().collect(Collectors.toList());
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attributeMap = importMetadata
                .getAnnotationAttributes(EnableCustomRedisHttpSession.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
        this.maxInactiveIntervalInSeconds = attributes.getNumber("maxInactiveIntervalInSeconds");
        String redisNamespaceValue = attributes.getString("redisNamespace");
        if (Strings.isNotBlank(redisNamespaceValue)) {
            this.redisNamespace = this.embeddedValueResolver.resolveStringValue(redisNamespaceValue);
        }
        FlushMode flushMode = attributes.getEnum("flushMode");
        RedisFlushMode redisFlushMode = attributes.getEnum("redisFlushMode");
        if (flushMode == FlushMode.ON_SAVE && redisFlushMode != RedisFlushMode.ON_SAVE) {
            flushMode = redisFlushMode.getFlushMode();
        }
        this.flushMode = flushMode;
        this.saveMode = attributes.getEnum("saveMode");
        String cleanupCron = attributes.getString("cleanupCron");
        if (Strings.isNotBlank(cleanupCron)) {
            this.cleanupCron = cleanupCron;
        }
    }

    private RedisTemplate<Object, Object> createRedisTemplate() {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        if (this.defaultRedisSerializer != null) {
            redisTemplate.setDefaultSerializer(this.defaultRedisSerializer);
        }
        redisTemplate.setConnectionFactory(this.redisConnectionFactory);
        redisTemplate.setBeanClassLoader(this.classLoader);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private int resolveDatabase() {
        if (ClassUtils.isPresent("io.lettuce.core.RedisClient", null)
                && this.redisConnectionFactory instanceof LettuceConnectionFactory) {
            return ((LettuceConnectionFactory) this.redisConnectionFactory).getDatabase();
        }
        if (ClassUtils.isPresent("redis.clients.jedis.Jedis", null)
                && this.redisConnectionFactory instanceof JedisConnectionFactory) {
            return ((JedisConnectionFactory) this.redisConnectionFactory).getDatabase();
        }
        return CustomSIDRedisIndexedSessionRepository.DEFAULT_DATABASE;
    }

    static class EnableRedisKeyspaceNotificationsInitializer implements InitializingBean {

        private final RedisConnectionFactory connectionFactory;

        private ConfigureRedisAction configure;

        EnableRedisKeyspaceNotificationsInitializer(RedisConnectionFactory connectionFactory,
                                                    ConfigureRedisAction configure) {
            this.connectionFactory = connectionFactory;
            this.configure = configure;
        }

        @Override
        public void afterPropertiesSet() {
            if (this.configure == ConfigureRedisAction.NO_OP) {
                return;
            }
            RedisConnection connection = this.connectionFactory.getConnection();
            try {
                this.configure.configure(connection);
            }
            finally {
                try {
                    connection.close();
                }
                catch (Exception ex) {
                    LoggerFactory.getLogger(getClass()).error("Error closing RedisConnection", ex);
                }
            }
        }

    }

    @EnableScheduling
    @Configuration(proxyBeanMethods = false)
    class SessionCleanupConfiguration implements SchedulingConfigurer {

        private final CustomSIDRedisIndexedSessionRepository sessionRepository;

        SessionCleanupConfiguration(CustomSIDRedisIndexedSessionRepository sessionRepository) {
            this.sessionRepository = sessionRepository;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            taskRegistrar.addCronTask(this.sessionRepository::cleanupExpiredSessions,
                    CustomRedisHttpSessionConfiguration.this.cleanupCron);
        }

    }

}
