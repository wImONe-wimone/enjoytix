package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.user.dto.req.UserOrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.OrderRemoteService;
import com.wimone.enjoytix.user.remote.dto.OrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.user.service.UserOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserOrderServiceImpl implements UserOrderService {

    private final OrderRemoteService orderRemoteService;

    public UserOrderServiceImpl(OrderRemoteService orderRemoteService) {
        this.orderRemoteService = orderRemoteService;
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
}
