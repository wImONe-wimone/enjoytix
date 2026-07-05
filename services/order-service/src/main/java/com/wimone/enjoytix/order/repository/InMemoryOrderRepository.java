package com.wimone.enjoytix.order.repository;

import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOrderRepository {

    private final Map<Long, OrderDO> orders = new ConcurrentHashMap<>();
    private final Map<Long, OrderItemDO> items = new ConcurrentHashMap<>();
    private final Map<Long, OrderStatusLogDO> statusLogs = new ConcurrentHashMap<>();

    public void saveOrder(OrderDO orderDO) {
        orders.put(orderDO.getId(), orderDO);
    }

    public Optional<OrderDO> findOrder(Long orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<OrderDO> listOrdersByUser(Long userId) {
        return orders.values()
                .stream()
                .filter(each -> userId.equals(each.getUserId()))
                .sorted(Comparator.comparing(OrderDO::getCreateTime).reversed())
                .toList();
    }

    public List<OrderDO> listOrders() {
        return new ArrayList<>(orders.values());
    }

    public void saveItem(OrderItemDO itemDO) {
        items.put(itemDO.getId(), itemDO);
    }

    public List<OrderItemDO> listItems(Long orderId) {
        return items.values()
                .stream()
                .filter(each -> orderId.equals(each.getOrderId()))
                .sorted(Comparator.comparing(OrderItemDO::getId))
                .toList();
    }

    public void saveStatusLog(OrderStatusLogDO logDO) {
        statusLogs.put(logDO.getId(), logDO);
    }
}
