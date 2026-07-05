package com.wimone.enjoytix.order.service;

import com.wimone.enjoytix.order.dto.req.OrderCancelReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderCreateReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.order.dto.resp.OrderCreateRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderDetailRespDTO;

import java.util.List;

public interface OrderService {

    OrderCreateRespDTO create(Long userId, OrderCreateReqDTO requestParam);

    Boolean cancel(Long userId, OrderCancelReqDTO requestParam);

    OrderDetailRespDTO paySuccess(OrderPaySuccessReqDTO requestParam);

    OrderDetailRespDTO detail(Long userId, Long orderId);

    List<OrderDetailRespDTO> list(Long userId);

    void closeExpiredOrders();
}
