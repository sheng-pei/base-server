package ppl.server.base.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import ppl.server.base.webmvc.response.RJsonResponseBodyAdvice;
import ppl.server.base.webmvc.response.Rcs;

@AutoConfiguration
public class RConfiguration {
    @Bean
    public Rcs rcs() {
        return new Rcs();
    }

    @Bean
    public RJsonResponseBodyAdvice rJsonResponseBodyAdvice() {
        return new RJsonResponseBodyAdvice();
    }
}
