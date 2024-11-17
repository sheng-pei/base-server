package ppl.server.base.webmvc;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResetBufferBeforeExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!response.isCommitted()) {
            response.resetBuffer();
        }
        return super.resolveException(request, response, handler, ex);
    }
}
