package com.wimone.enjoytix.agent.remote.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderDetailResponse(Long orderId, String orderSn, Long userId, Long showId, Long lockId, BigDecimal totalAmount, String status, LocalDateTime payExpireTime, List<OrderItemResponse> items) {}
