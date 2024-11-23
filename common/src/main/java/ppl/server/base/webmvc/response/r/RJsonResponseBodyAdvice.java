package ppl.server.base.webmvc.response.r;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import ppl.server.base.webmvc.response.jackson.JacksonResponseBody;

@ControllerAdvice
public class RJsonResponseBodyAdvice implements ResponseBodyAdvice<Object>, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Rcs rcs;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), JacksonResponseBody.class) ||
                returnType.hasMethodAnnotation(JacksonResponseBody.class)) &&
                AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse) {
            ServletServerHttpResponse sshr = ((ServletServerHttpResponse) response);
            int status = sshr.getServletResponse().getStatus();
            if (status == HttpStatus.OK.value()) {
                init();
                return rcs.ok()
                        .res()
                        .data(body);
            }
        }
        return body;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void init() {
        if (this.rcs == null) {
            Rcs rcs = null;
            try {
                rcs = applicationContext.getBean(Rcs.class);
            } catch (Throwable t) {
                //ignore
            }
            this.rcs = rcs == null ? new Rcs() : rcs;
        }
    }
}
