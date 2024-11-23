package ppl.server.base.webmvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import ppl.server.base.webmvc.response.r.R;
import ppl.server.base.webmvc.response.r.Rcs;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class CommonErrorController implements ErrorController, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(CommonErrorController.class);

    private final Rcs rcs;
    private final ErrorAttributes errorAttributes;
    private HttpStatus statusOnException;

    public CommonErrorController(Rcs rcs, ErrorAttributes errorAttributes) {
        Objects.requireNonNull(rcs, "rcs is required.");
        Objects.requireNonNull(errorAttributes, "errorAttributes is required.");
        this.rcs = rcs;
        this.errorAttributes = errorAttributes;
    }

    @Value(value = "${common.error.status.on.exception:}")
    public void setDefaultStatusForException(Integer statusOnException) {
        HttpStatus status = HttpStatus.INSUFFICIENT_STORAGE;
        try {
            status = (statusOnException == null ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.valueOf(statusOnException));
        } catch (Throwable t) {
            //ignore
            log.warn("Unknown status code: " + statusOnException +
                    ". Use " + HttpStatus.INTERNAL_SERVER_ERROR.value() + " for default.", t);
        }
        this.statusOnException = status;
    }

    @RequestMapping
    public ResponseEntity<R<?>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }

        if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            return new ResponseEntity<>(rcs.methodNotAllowed().res(), status);
        }

        WebRequest webRequest = new ServletWebRequest(request);
        if (status == HttpStatus.NOT_FOUND) {
            R<Void> notFound = rcs.notFound().res();
            return new ResponseEntity<>(notFound, status);
        }

        Throwable err = errorAttributes.getError(webRequest);
        try {
            if (err == null) {
                err = (Throwable) webRequest.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION,
                        RequestAttributes.SCOPE_REQUEST);
            }
            if (err == null) {
                err = (Throwable) webRequest.getAttribute(WebAttributes.ACCESS_DENIED_403,
                        RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Throwable t) {
            //ignore ClassNotFoundException
        }

        if (err != null) {
            log.info("Service error: " + request.getRequestURI(), err);
            return new ResponseEntity<>(rcs.fromException(err), statusOnException);
        }

        return new ResponseEntity<>(status);
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            return null;
        }

        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            log.debug("Unknown status code: " + statusCode + ".", ex);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(errorAttributes, "error attributes is required.");
    }
}
