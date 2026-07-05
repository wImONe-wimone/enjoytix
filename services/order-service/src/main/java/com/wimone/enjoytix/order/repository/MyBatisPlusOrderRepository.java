package com.wimone.enjoytix.order.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import com.wimone.enjoytix.order.dao.mapper.OrderItemMapper;
import com.wimone.enjoytix.order.dao.mapper.OrderMapper;
import com.wimone.enjoytix.order.dao.mapper.OrderStatusLogMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    public MyBatisPlusOrderRepository(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            OrderStatusLogMapper orderStatusLogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
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
}
