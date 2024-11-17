package ppl.server.base.session;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.FlushMode;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(CustomRedisHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableCustomRedisHttpSession {

    int maxInactiveIntervalInSeconds() default MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    String redisNamespace() default RedisIndexedSessionRepository.DEFAULT_NAMESPACE;

    @Deprecated
    RedisFlushMode redisFlushMode() default RedisFlushMode.ON_SAVE;

    FlushMode flushMode() default FlushMode.ON_SAVE;

    String cleanupCron() default CustomRedisHttpSessionConfiguration.DEFAULT_CLEANUP_CRON;

    SaveMode saveMode() default SaveMode.ON_SET_ATTRIBUTE;

}
