package com.wimone.enjoytix.order.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.order.common.enums.OrderTimeoutMessageStatusEnum;
import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import com.wimone.enjoytix.order.dao.entity.OrderTimeoutMessageLogDO;
import com.wimone.enjoytix.order.dao.mapper.OrderItemMapper;
import com.wimone.enjoytix.order.dao.mapper.OrderMapper;
import com.wimone.enjoytix.order.dao.mapper.OrderStatusLogMapper;
import com.wimone.enjoytix.order.dao.mapper.OrderTimeoutMessageLogMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderTimeoutMessageLogMapper orderTimeoutMessageLogMapper;

    public MyBatisPlusOrderRepository(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            OrderStatusLogMapper orderStatusLogMapper,
            OrderTimeoutMessageLogMapper orderTimeoutMessageLogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.orderTimeoutMessageLogMapper = orderTimeoutMessageLogMapper;
    }

    @Override
    public void saveOrder(OrderDO orderDO) {
        if (orderMapper.selectById(orderDO.getId()) == null) {
            orderMapper.insert(orderDO);
            return;
        }
        orderMapper.updateById(orderDO);
    }

    @Override
    public Optional<OrderDO> findOrder(Long orderId) {
        return Optional.ofNullable(orderMapper.selectById(orderId));
    }

    @Override
    public List<OrderDO> listOrdersByUser(Long userId) {
        return orderMapper.selectList(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, userId)
                .orderByDesc(OrderDO::getCreateTime));
    }

    @Override
    public List<OrderDO> listOrders() {
        return orderMapper.selectList(Wrappers.lambdaQuery(OrderDO.class)
                .orderByDesc(OrderDO::getCreateTime));
    }

    @Override
    public void saveItem(OrderItemDO itemDO) {
        if (orderItemMapper.selectById(itemDO.getId()) == null) {
            orderItemMapper.insert(itemDO);
            return;
        }
        orderItemMapper.updateById(itemDO);
    }

    @Override
    public List<OrderItemDO> listItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderId, orderId)
                .orderByAsc(OrderItemDO::getId));
    }

    @Override
    public void saveStatusLog(OrderStatusLogDO logDO) {
        if (orderStatusLogMapper.selectById(logDO.getId()) == null) {
            orderStatusLogMapper.insert(logDO);
            return;
        }
        orderStatusLogMapper.updateById(logDO);
    }

    @Override
    public void saveTimeoutMessageLog(OrderTimeoutMessageLogDO logDO) {
        if (logDO.getId() != null && orderTimeoutMessageLogMapper.selectById(logDO.getId()) != null) {
            orderTimeoutMessageLogMapper.updateById(logDO);
            return;
        }
        Optional<OrderTimeoutMessageLogDO> existing = findTimeoutMessageLog(logDO.getMessageKey());
        if (existing.isPresent()) {
            logDO.setId(existing.get().getId());
            orderTimeoutMessageLogMapper.updateById(logDO);
            return;
        }
        orderTimeoutMessageLogMapper.insert(logDO);
    }

    @Override
    public void saveTimeoutMessageLogIfAbsent(OrderTimeoutMessageLogDO logDO) {
        try {
            orderTimeoutMessageLogMapper.insert(logDO);
        } catch (DuplicateKeyException ignored) {
            // Another consumer has already created the idempotent consume record.
        }
    }

    @Override
    public Optional<OrderTimeoutMessageLogDO> findTimeoutMessageLog(String messageKey) {
        return Optional.ofNullable(orderTimeoutMessageLogMapper.selectOne(Wrappers.lambdaQuery(OrderTimeoutMessageLogDO.class)
                .eq(OrderTimeoutMessageLogDO::getMessageKey, messageKey)
                .last("LIMIT 1")));
    }

    @Override
    public List<OrderTimeoutMessageLogDO> listTimeoutMessageLogsForCompensation(LocalDateTime now, int limit) {
        return orderTimeoutMessageLogMapper.selectList(Wrappers.lambdaQuery(OrderTimeoutMessageLogDO.class)
                .ne(OrderTimeoutMessageLogDO::getStatus, OrderTimeoutMessageStatusEnum.SUCCESS.name())
                .le(OrderTimeoutMessageLogDO::getExpireTime, now)
                .orderByAsc(OrderTimeoutMessageLogDO::getExpireTime)
                .last("LIMIT " + Math.max(1, limit)));
    }
}
