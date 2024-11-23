package ppl.server.base.webmvc.response.r;

import ppl.common.utils.ArrayUtils;
import ppl.common.utils.string.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class ResponseCode {

    private final int code;
    private final boolean success;
    private final String message;
    private final Set<Class<? extends Throwable>> throwables;

    @SafeVarargs
    ResponseCode(int code, boolean success, String message, Class<? extends Throwable>... ts) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.throwables = Collections.unmodifiableSet(Arrays.stream(ts).collect(Collectors.toSet()));
    }

    @SafeVarargs
    ResponseCode(int code, String message, Class<? extends Throwable>... ts) {
        this(code, false, message, ts);
    }

    public <D> R<D> res(MessageParameter messageParameter) {
        return res(this, messageParameter.params());
    }

    public <D> R<D> res(Object... info) {
        return new R<>(this, info);
    }

    public int code() {
        return code;
    }

    public boolean success() {
        return success;
    }

    public String message(Object... info) {
        return Strings.format(message, info);
    }

    public Class<? extends Throwable>[] throwables() {
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] ret = throwables.toArray(ArrayUtils.zero(Class.class));
        return ret;
    }
}
