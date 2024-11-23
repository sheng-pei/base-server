package ppl.server.base.webmvc;

import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import ppl.server.base.webmvc.response.jackson.JacksonResponseBodyReturnValueHandler;

import java.util.ArrayList;
import java.util.List;

public class AdviceAccessibleRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    private final List<Object> advices = new ArrayList<>();
    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

    @Override
    public void setRequestBodyAdvice(List<RequestBodyAdvice> requestBodyAdvice) {
        super.setRequestBodyAdvice(requestBodyAdvice);
        if (requestBodyAdvice != null) {
            advices.addAll(requestBodyAdvice);
        }
    }

    @Override
    public void setResponseBodyAdvice(List<ResponseBodyAdvice<?>> responseBodyAdvice) {
        super.setResponseBodyAdvice(responseBodyAdvice);
        if (responseBodyAdvice != null) {
            advices.addAll(responseBodyAdvice);
        }
    }

    @Override
    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        super.setContentNegotiationManager(contentNegotiationManager);
        this.contentNegotiationManager = contentNegotiationManager;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        initControllerAdviceCache();
        List<HandlerMethodReturnValueHandler> newHandlers = new ArrayList<>();
        List<HandlerMethodReturnValueHandler> handlers = getReturnValueHandlers();
        if (handlers != null) {
            for (HandlerMethodReturnValueHandler handler : handlers) {
                if (handler instanceof RequestResponseBodyMethodProcessor) {
                    newHandlers.add(new JacksonResponseBodyReturnValueHandler(getMessageConverters(), contentNegotiationManager, advices));
                }
                newHandlers.add(handler);
            }
        }
        setReturnValueHandlers(newHandlers);
    }

    private void initControllerAdviceCache() {
        if (getApplicationContext() == null) {
            return;
        }

        List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());

        List<Object> requestResponseBodyAdviceBeans = new ArrayList<>();

        for (ControllerAdviceBean adviceBean : adviceBeans) {
            Class<?> beanType = adviceBean.getBeanType();
            if (beanType == null) {
                throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
            }
            if (RequestBodyAdvice.class.isAssignableFrom(beanType) || ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
                requestResponseBodyAdviceBeans.add(adviceBean);
            }
        }

        if (!requestResponseBodyAdviceBeans.isEmpty()) {
            this.advices.addAll(0, requestResponseBodyAdviceBeans);
        }
    }
}
