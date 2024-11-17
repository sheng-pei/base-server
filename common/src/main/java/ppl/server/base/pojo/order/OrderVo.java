package ppl.server.base.pojo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import ppl.server.base.enums.OrderDirection;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 排序参数
 */
@Schema(description = "排序参数")
public class OrderVo {
    public static final OrderDirection DEF_DIRECTION = OrderDirection.ASC;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段")
    @NotBlank(message = "排序字段不能为空")
    private String field;

    /**
     * 排序方向：asc/desc
     */
    @Schema(description = "排序方向：asc/desc, 忽略大小写")
    @NotNull(message = "排序方向不能为NULL")
    private OrderDirection direction;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public OrderDirection getDirection() {
        return direction == null ? DEF_DIRECTION : direction;
    }

    public void setDirection(String direction) {
        if (direction != null) {
            this.direction = OrderDirection.enumOf(direction);
        }
    }
}
