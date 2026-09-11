package com.wimone.enjoytix.order.service;

import com.wimone.enjoytix.order.dto.req.OrderCancelReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderCreateReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderRefundApplyReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderRefundCompleteReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderRefundRollbackReqDTO;
import com.wimone.enjoytix.order.dto.resp.OrderCreateRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderDetailRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderPurchaseCheckRespDTO;

import java.util.List;

public interface OrderService {

    OrderCreateRespDTO create(Long userId, OrderCreateReqDTO requestParam);

    Boolean cancel(Long userId, OrderCancelReqDTO requestParam);

    OrderDetailRespDTO applyRefund(Long userId, OrderRefundApplyReqDTO requestParam);

    OrderDetailRespDTO completeRefund(Long userId, OrderRefundCompleteReqDTO requestParam);

    OrderDetailRespDTO rollbackRefund(Long userId, OrderRefundRollbackReqDTO requestParam);

    OrderDetailRespDTO paySuccess(OrderPaySuccessReqDTO requestParam);

    OrderDetailRespDTO detail(Long userId, Long orderId);

    List<OrderDetailRespDTO> list(Long userId);

    OrderPurchaseCheckRespDTO checkPurchase(Long userId, Long performanceId);

    void closeExpiredOrders();
}
