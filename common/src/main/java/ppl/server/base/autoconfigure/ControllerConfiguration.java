package ppl.server.base.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import ppl.server.base.webmvc.controller.CommonErrorController;
import ppl.server.base.webmvc.controller.CommonOutputController;
import ppl.server.base.webmvc.response.Rcs;

@AutoConfiguration(before = ErrorMvcAutoConfiguration.class)
public class ControllerConfiguration {
    @Bean
    public CommonErrorController commonErrorController(Rcs rcs, ErrorAttributes errorAttributes) {
        return new CommonErrorController(rcs, errorAttributes);
    }

    @Bean
    public CommonOutputController commonSuccessController(Rcs rcs) {
        return new CommonOutputController(rcs);
    }
}
