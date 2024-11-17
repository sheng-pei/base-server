package ppl.server.base.webmvc.response;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@JacksonResponseBody
public @interface RController {
    @AliasFor(annotation = Controller.class)
    String value() default "";
}
