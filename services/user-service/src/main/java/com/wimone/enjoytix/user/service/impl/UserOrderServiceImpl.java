package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.user.dto.req.UserOrderCancelReqDTO;
import com.wimone.enjoytix.user.dto.req.UserOrderRefundReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserOrderRefundRespDTO;
import com.wimone.enjoytix.user.remote.OrderRemoteService;
import com.wimone.enjoytix.user.remote.PayRemoteService;
import com.wimone.enjoytix.user.remote.dto.OrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundApplyReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundCompleteReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderRefundRollbackReqDTO;
import com.wimone.enjoytix.user.remote.dto.RefundByOrderReqDTO;
import com.wimone.enjoytix.user.remote.dto.RefundRespDTO;
import com.wimone.enjoytix.user.service.UserOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserOrderServiceImpl implements UserOrderService {

    private static final Logger log = LoggerFactory.getLogger(UserOrderServiceImpl.class);
    private static final int REFUND_COMPLETE_MAX_ATTEMPTS = 3;

    private final OrderRemoteService orderRemoteService;
    private final PayRemoteService payRemoteService;

    public UserOrderServiceImpl(OrderRemoteService orderRemoteService, PayRemoteService payRemoteService) {
        this.orderRemoteService = orderRemoteService;
        this.payRemoteService = payRemoteService;
    }

    @Override
    public List<OrderDetailRespDTO> list(Long userId) {
        Result<List<OrderDetailRespDTO>> result = orderRemoteService.list(userId);
        if (!result.isSuccess()) {
            throw new RemoteException("Query user orders failed: " + result.getMessage());
        }
        return result.getData() == null ? List.of() : result.getData();
    }

    @Override
    public OrderDetailRespDTO detail(Long userId, Long orderId) {
        Result<OrderDetailRespDTO> result = orderRemoteService.detail(userId, orderId);
        if (!result.isSuccess()) {
            throw new RemoteException("Query order detail failed: " + result.getMessage());
        }
        return result.getData();
    }

    @Override
    public Boolean cancel(Long userId, UserOrderCancelReqDTO requestParam) {
        Result<Boolean> result = orderRemoteService.cancel(userId, new OrderCancelReqDTO(requestParam.getOrderId()));
        if (!result.isSuccess()) {
            throw new RemoteException("Cancel order failed: " + result.getMessage());
        }
        return Boolean.TRUE.equals(result.getData());
    }

    @Override
    public UserOrderRefundRespDTO refund(Long userId, UserOrderRefundReqDTO requestParam) {
        OrderDetailRespDTO applyingOrder = applyOrderRefund(userId, requestParam);
        assertOrderOwner(userId, applyingOrder);

        RefundRespDTO refund = refundPayOrder(userId, requestParam);
        OrderDetailRespDTO completedOrder = completeOrderRefundWithRetry(userId, requestParam.getOrderId(), refund.refundId());
        assertOrderOwner(userId, completedOrder);
        return new UserOrderRefundRespDTO(
                completedOrder.orderId(),
                refund.payId(),
                refund.refundId(),
                refund.amount(),
                refund.status(),
                completedOrder.status()
        );
    }

    private OrderDetailRespDTO applyOrderRefund(Long userId, UserOrderRefundReqDTO requestParam) {
        Result<OrderDetailRespDTO> result;
        try {
            result = orderRemoteService.applyRefund(
                    userId,
                    new OrderRefundApplyReqDTO(requestParam.getOrderId(), requestParam.getReason())
            );
        } catch (RuntimeException ex) {
            log.warn("order_refund_apply_unknown userId={} orderId={}", userId, requestParam.getOrderId(), ex);
            throw new RemoteException("Order refund application result is unknown; retry refund API to continue. Last error: " + ex.getMessage());
        }
        if (!result.isSuccess()) {
            throw new RemoteException("Apply order refund failed: " + result.getMessage());
        }
        return result.getData();
    }

    private RefundRespDTO refundPayOrder(Long userId, UserOrderRefundReqDTO requestParam) {
        Result<RefundRespDTO> result;
        try {
            result = payRemoteService.applyByOrder(
                    userId,
                    new RefundByOrderReqDTO(requestParam.getOrderId(), requestParam.getReason())
            );
        } catch (RuntimeException ex) {
            log.warn("pay_refund_unknown userId={} orderId={}", userId, requestParam.getOrderId(), ex);
            throw new RemoteException("Refund processing result is unknown; retry refund API to continue or compensate. Last error: " + ex.getMessage());
        }
        if (!result.isSuccess()) {
            try {
                rollbackOrderRefund(userId, requestParam.getOrderId(), "pay refund failed: " + result.getMessage());
            } catch (RuntimeException rollbackEx) {
                throw new RemoteException("Process refund failed and order rollback failed. Refund error: "
                        + result.getMessage() + "; rollback error: " + rollbackEx.getMessage());
            }
            throw new RemoteException("Process refund failed: " + result.getMessage());
        }
        return result.getData();
    }

    private OrderDetailRespDTO completeOrderRefundWithRetry(Long userId, Long orderId, Long refundId) {
        RuntimeException lastException = null;
        String lastFailureMessage = null;
        for (int attempt = 1; attempt <= REFUND_COMPLETE_MAX_ATTEMPTS; attempt++) {
            try {
                Result<OrderDetailRespDTO> result = orderRemoteService.completeRefund(
                        userId,
                        new OrderRefundCompleteReqDTO(orderId, refundId)
                );
                if (result.isSuccess()) {
                    return result.getData();
                }
                lastFailureMessage = result.getMessage();
                log.warn("order_refund_complete_failed userId={} orderId={} attempt={} message={}",
                        userId, orderId, attempt, result.getMessage());
            } catch (RuntimeException ex) {
                lastException = ex;
                log.warn("order_refund_complete_exception userId={} orderId={} attempt={}",
                        userId, orderId, attempt, ex);
            }
        }
        String message = lastException == null ? lastFailureMessage : lastException.getMessage();
        throw new RemoteException("Refund succeeded but order cancellation or seat release failed; retry refund API to resume completion. Last error: " + message);
    }

    private void rollbackOrderRefund(Long userId, Long orderId, String reason) {
        try {
            Result<OrderDetailRespDTO> result = orderRemoteService.rollbackRefund(
                    userId,
                    new OrderRefundRollbackReqDTO(orderId, reason)
            );
            if (!result.isSuccess()) {
                throw new RemoteException("Rollback order refund failed: " + result.getMessage());
            }
        } catch (RuntimeException rollbackEx) {
            log.error("order_refund_rollback_failed userId={} orderId={}", userId, orderId, rollbackEx);
            throw rollbackEx;
        }
    }

    private void assertOrderOwner(Long userId, OrderDetailRespDTO order) {
        if (order == null) {
            throw new RemoteException("Order service returned empty order data");
        }
        if (!userId.equals(order.userId())) {
            throw new ClientException("Order does not belong to current user");
        }
    }
}
