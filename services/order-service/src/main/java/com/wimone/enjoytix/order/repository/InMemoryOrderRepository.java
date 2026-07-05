package com.wimone.enjoytix.order.repository;

import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, OrderDO> orders = new ConcurrentHashMap<>();
    private final Map<Long, OrderItemDO> items = new ConcurrentHashMap<>();
    private final Map<Long, OrderStatusLogDO> statusLogs = new ConcurrentHashMap<>();

    @Override
    public void saveOrder(OrderDO orderDO) {
        orders.put(orderDO.getId(), orderDO);
    }

    @Override
    public Optional<OrderDO> findOrder(Long orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<OrderDO> listOrdersByUser(Long userId) {
        return orders.values()
                .stream()
                .filter(each -> userId.equals(each.getUserId()))
                .sorted(Comparator.comparing(OrderDO::getCreateTime).reversed())
                .toList();
    }

    @Override
    public List<OrderDO> listOrders() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public void saveItem(OrderItemDO itemDO) {
        items.put(itemDO.getId(), itemDO);
    }

    @Override
    public List<OrderItemDO> listItems(Long orderId) {
        return items.values()
                .stream()
                .filter(each -> orderId.equals(each.getOrderId()))
                .sorted(Comparator.comparing(OrderItemDO::getId))
                .toList();
    }

    @Override
    public void saveStatusLog(OrderStatusLogDO logDO) {
        statusLogs.put(logDO.getId(), logDO);
    }
}
