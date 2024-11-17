package ppl.server.base.autoconfigure;

import com.fasterxml.jackson.databind.Module;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import ppl.common.utils.json.jackson.CommonModule;
import ppl.common.utils.json.jackson.JavaTimeModule;

@AutoConfiguration
public class CommonJacksonConfiguration {
    @Bean
    Module commonModule() {
        return new CommonModule();
    }

    @Bean
    @ConditionalOnClass(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule.class)
    Module javaTimeModule() {
        return new JavaTimeModule();
    }
}
