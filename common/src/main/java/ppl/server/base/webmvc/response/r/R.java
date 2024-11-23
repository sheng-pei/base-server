package ppl.server.base.webmvc.response.r;

public class R<T> {
    private final ResponseCode code;
    private String message;
    private T data;

    public R(ResponseCode code, Object... params) {
        this.code = code;
        this.message = code.message(params);
    }

    public R<T> message(String message) {
        this.message = message;
        return this;
    }

    public <D> R<D> data(D data) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        R<D> res = (R) this;
        res.data = data;
        return res;
    }

    public ResponseCode code() {
        return code;
    }

    public int getCode() {
        return code.code();
    }

    public boolean isSuccess() {
        return code.success();
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

}
