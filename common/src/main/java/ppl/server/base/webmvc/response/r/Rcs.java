package ppl.server.base.webmvc.response.r;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestValueException;
import ppl.common.utils.ArrayUtils;

import javax.validation.ConstraintViolationException;
import java.util.*;
import java.util.function.Supplier;

//TODO, please provide auto register util
public class Rcs {
    private static final Logger log = LoggerFactory.getLogger(Rcs.class);

    private static final Set<ResponseCode> DEFAULT_CODES = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final ResponseCode OK = registerDefault(1, true, "成功。");
    private static final ResponseCode REDIRECTION = registerDefault(2, true, "重定向。");

    //error
    private static final ResponseCode UNKNOWN = registerDefault(1000, false, "未知错误。", Throwable.class);
    private static final ResponseCode DATA_ERR = registerDefault(1001, false, "数据访问异常。");
    private static final ResponseCode RESOURCE_NOT_FOUND = registerDefault(1002, false, "资源未找到。");
    private static final ResponseCode METHOD_NOT_ALLOWED = registerDefault(1003, false, "方法不被允许。");

    private static final ResponseCode NO_PERMISSION_ERROR = typeSafeCall(() ->
            registerDefault(10000, false, "无权限。", AccessDeniedException.class));

    //application error
    private static final ResponseCode REQUEST_CONSTRAINT_VIOLATION_ERROR = typeSafeCall(() ->
            registerDefault(20000, false, "不合法请求。",
                    ConstraintViolationException.class,
                    MissingRequestValueException.class));


    @SafeVarargs
    private static ResponseCode registerDefault(int code, boolean success, String message, Class<? extends Throwable>... ts) {
        ResponseCode rc = new ResponseCode(code, success, message, ts);
        DEFAULT_CODES.add(rc);
        return rc;
    }

    private static ResponseCode typeSafeCall(Supplier<ResponseCode> supplier) {
        try {
            return supplier.get();
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    private Map<Class<? extends Throwable>, ResponseCode> exception2ResponseCode;
    private final Map<Integer, ResponseCode> responseCodes = new TreeMap<>();

    public Rcs() {
        for (ResponseCode defaultCode : DEFAULT_CODES) {
            register(defaultCode);
        }
    }

    @SafeVarargs
    public final ResponseCode register(int code, String message, Class<? extends Throwable>... ts) {
        return register(code, false, message, ts);
    }

    @SafeVarargs
    public final ResponseCode register(int code, boolean success, String message, Class<? extends Throwable>... ts) {
        ResponseCode existed = ignoreIfNotDefault(code);
        ResponseCode rc = new ResponseCode(code, success, message, ts);
        return existed == null ? putResponseCode(rc) : existed;
    }

    private void register(ResponseCode code) {
        if (code == null) {
            return;
        }

        ResponseCode existed = ignoreIfNotDefault(code.code());
        if (existed == null) {
            putResponseCode(code);
        }
    }

    private ResponseCode ignoreIfNotDefault(int code) {
        if (responseCodes.containsKey(code)) {
            ResponseCode rc = responseCodes.get(code);
            if (!DEFAULT_CODES.contains(rc)) {
                log.warn("Existed non-default response code: '" + code + "'. Ignore new code.");
                return rc;
            }
        }
        return null;
    }

    private ResponseCode putResponseCode(ResponseCode rc) {
        responseCodes.put(rc.code(), rc);
        return rc;
    }

    public ResponseCode ok() {
        return responseCodes.get(OK.code());
    }

    public ResponseCode redirect() {
        return responseCodes.get(REDIRECTION.code());
    }

    public ResponseCode notFound() {
        return responseCodes.get(RESOURCE_NOT_FOUND.code());
    }

    public ResponseCode methodNotAllowed() {
        return responseCodes.get(METHOD_NOT_ALLOWED.code());
    }

    public ResponseCode unknown() {
        return responseCodes.get(UNKNOWN.code());
    }

    public <T> R<T> success(T data) {
        return ok().res().data(data);
    }

    public <T> R<T> fromException(Throwable t) {
        ResponseCode rc = pFromException(t);
        Object[] params = ArrayUtils.zero();
        if (t instanceof MessageParameter) {
            params = ((MessageParameter) t).params();
        }
        return rc.res(params);
    }

    private ResponseCode pFromException(Throwable t) {
        Map<Class<? extends Throwable>, ResponseCode> exception2ResponseCode = this.exception2ResponseCode;
        if (exception2ResponseCode == null) {
            exception2ResponseCode = new HashMap<>();
            for (ResponseCode rc : responseCodes.values()) {
                for (Class<? extends Throwable> clazz : rc.throwables()) {
                    if (exception2ResponseCode.containsKey(clazz)) {
                        log.warn("Ignore existed exception: '" + clazz.getCanonicalName() + "'.");
                    } else {
                        exception2ResponseCode.put(clazz, rc);
                    }
                }
            }
            this.exception2ResponseCode = Collections.unmodifiableMap(exception2ResponseCode);
        }

        Class<?> clazz = t.getClass();
        while (!Object.class.equals(clazz)) {
            ResponseCode rc = exception2ResponseCode.get(clazz);
            if (rc != null) {
                return rc;
            }
            clazz = clazz.getSuperclass();
        }
        throw new IllegalArgumentException("Unknown throwable.", t);
    }
}
