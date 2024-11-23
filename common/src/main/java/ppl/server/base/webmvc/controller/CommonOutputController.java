package ppl.server.base.webmvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("${server.output.path:${output.path:/output}}")
public class CommonOutputController {

    private static final Logger log = LoggerFactory.getLogger(CommonOutputController.class);

    public static final String FILTER_SUCCESS_BODY = "FILTER_SUCCESS_BODY";
    public static final String FILTER_SUCCESS_MESSAGE = "FILTER_SUCCESS_MESSAGE";

    private final Rcs rcs;

    public CommonOutputController(Rcs rcs) {
        Objects.requireNonNull(rcs, "rcs is required.");
        this.rcs = rcs;
    }

    @RequestMapping
    public ResponseEntity<R<?>> success(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status != HttpStatus.OK) {
            return new ResponseEntity<>(rcs.unknown().res(), HttpStatus.OK);
        }

        WebRequest webRequest = new ServletWebRequest(request);
        Object msg = webRequest.getAttribute(FILTER_SUCCESS_MESSAGE,
                RequestAttributes.SCOPE_REQUEST);
        Object body = webRequest.getAttribute(FILTER_SUCCESS_BODY,
                RequestAttributes.SCOPE_REQUEST);
        webRequest.removeAttribute(FILTER_SUCCESS_MESSAGE, RequestAttributes.SCOPE_REQUEST);
        webRequest.removeAttribute(FILTER_SUCCESS_BODY, RequestAttributes.SCOPE_REQUEST);
        R<?> ret = rcs.ok().res();
        if (msg != null) {
            ret.message(msg.toString());
        }
        ret.data(body);
        return new ResponseEntity<>(ret, status);
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        HttpStatus status;
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            status = HttpStatus.OK;
        } else {
            try {
                status = HttpStatus.valueOf(statusCode);
            } catch (Exception ex) {
                log.debug("Unknown status code: " + statusCode + ".", ex);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return status != HttpStatus.OK ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }
}
