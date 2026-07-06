package com.wimone.enjoytix.user.remote.dto;

public record RefundByOrderReqDTO(Long orderId, String reason) {
}
