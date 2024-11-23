package ppl.server.base.webmvc.response.jackson;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JacksonResponseBodyReturnValueHandler extends RequestResponseBodyMethodProcessor {

    public JacksonResponseBodyReturnValueHandler(List<HttpMessageConverter<?>> converters,
                                                 @Nullable ContentNegotiationManager manager,
                                                 @Nullable List<Object> requestResponseBodyAdvice) {
        super(jackson(converters), manager, requestResponseBodyAdvice);
    }

    private static List<HttpMessageConverter<?>> jackson(List<HttpMessageConverter<?>> converters) {
        List<HttpMessageConverter<?>> ret = new ArrayList<>();
        List<HttpMessageConverter<?>> base = new ArrayList<>();
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof ByteArrayHttpMessageConverter ||
                    converter instanceof StringHttpMessageConverter) {
                base.add(converter);
            } else {
                ret.add(converter);
            }
        }
        ret.addAll(base);
        return Collections.unmodifiableList(ret);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return false;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), JacksonResponseBody.class) ||
                returnType.hasMethodAnnotation(JacksonResponseBody.class));
    }
}
