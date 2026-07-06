package com.wimone.enjoytix.user.service;

import com.wimone.enjoytix.user.dto.req.UserOrderCancelReqDTO;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;

import java.util.List;

public interface UserOrderService {

    List<OrderDetailRespDTO> list(Long userId);

    OrderDetailRespDTO detail(Long userId, Long orderId);

    Boolean cancel(Long userId, UserOrderCancelReqDTO requestParam);
}
