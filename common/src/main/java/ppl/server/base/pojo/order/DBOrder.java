package ppl.server.base.pojo.order;

import ppl.server.base.enums.OrderDirection;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface DBOrder {
    String getColumn();
    OrderDirection getDirection();

    static DBOrder mysql(String field, String direction) {
        OrderVo order = new OrderVo();
        order.setField(field);
        order.setDirection(direction);
        return mysql(order);
    }

    static DBOrder mysql(OrderVo order) {
        return new MysqlOrder(order);
    }

    static List<DBOrder> mysql(List<OrderVo> orders) {
        if (orders == null) {
            return Collections.emptyList();
        }

        return orders.stream().map(DBOrder::mysql).collect(Collectors.toList());
    }
}
