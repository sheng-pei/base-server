package ppl.server.base.enums;

import java.util.Objects;

/**
 * 排序方向
 */
public enum OrderDirection {
    /** 升序 */ ASC, /** 降序 */ DESC;

    public static OrderDirection enumOf(String direction) {
        Objects.requireNonNull(direction);
        return OrderDirection.valueOf(direction.toUpperCase());
    }
}
