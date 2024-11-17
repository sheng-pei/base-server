package ppl.server.base.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import ppl.server.base.webmvc.AdviceAccessibleRequestMappingHandlerAdapter;
import ppl.server.base.webmvc.ResetBufferBeforeExceptionHandlerExceptionResolver;

@AutoConfiguration
public class WebMvcConfiguration {
    @Bean
    WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
                return new AdviceAccessibleRequestMappingHandlerAdapter();
            }

            @Override
            public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
                return new ResetBufferBeforeExceptionHandlerExceptionResolver();
            }
        };
    }
}
