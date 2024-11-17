package ppl.server.base.pojo.order;

import ppl.common.utils.string.Strings;

public class MysqlOrder implements DBOrder {
    private final OrderVo order;

    public MysqlOrder(OrderVo order) {
        this.order = order;
    }

    @Override
    public String getColumn() {
        return Strings.toSnakeCase(order.getField());
    }

    @Override
    public OrderDirection getDirection() {
        return order.getDirection();
    }
}
