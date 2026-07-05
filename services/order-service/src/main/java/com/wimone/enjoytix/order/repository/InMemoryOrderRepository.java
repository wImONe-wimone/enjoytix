package com.wimone.enjoytix.order.repository;

import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import com.wimone.enjoytix.order.dao.entity.OrderTimeoutMessageLogDO;
import com.wimone.enjoytix.order.common.enums.OrderTimeoutMessageStatusEnum;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    private final Map<String, OrderTimeoutMessageLogDO> timeoutMessageLogs = new ConcurrentHashMap<>();

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

    @Override
    public void saveTimeoutMessageLog(OrderTimeoutMessageLogDO logDO) {
        timeoutMessageLogs.put(logDO.getMessageKey(), logDO);
    }

    @Override
    public void saveTimeoutMessageLogIfAbsent(OrderTimeoutMessageLogDO logDO) {
        timeoutMessageLogs.putIfAbsent(logDO.getMessageKey(), logDO);
    }

    @Override
    public Optional<OrderTimeoutMessageLogDO> findTimeoutMessageLog(String messageKey) {
        return Optional.ofNullable(timeoutMessageLogs.get(messageKey));
    }

    @Override
    public List<OrderTimeoutMessageLogDO> listTimeoutMessageLogsForCompensation(LocalDateTime now, int limit) {
        return timeoutMessageLogs.values()
                .stream()
                .filter(each -> !OrderTimeoutMessageStatusEnum.SUCCESS.name().equals(each.getStatus()))
                .filter(each -> !each.getExpireTime().isAfter(now))
                .sorted(Comparator.comparing(OrderTimeoutMessageLogDO::getExpireTime))
                .limit(limit)
                .toList();
    }
}
