package com.wimone.enjoytix.order.repository;

import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void saveOrder(OrderDO orderDO);

    Optional<OrderDO> findOrder(Long orderId);

    List<OrderDO> listOrdersByUser(Long userId);

    List<OrderDO> listOrders();

    void saveItem(OrderItemDO itemDO);

    List<OrderItemDO> listItems(Long orderId);

    void saveStatusLog(OrderStatusLogDO logDO);
}
