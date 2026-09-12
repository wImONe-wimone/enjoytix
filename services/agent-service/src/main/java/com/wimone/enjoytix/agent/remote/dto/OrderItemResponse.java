package com.wimone.enjoytix.agent.remote.dto;
import java.math.BigDecimal;
import java.util.List;
public record OrderItemResponse(Long itemId, Long showId, Long categoryId, Integer quantity, List<Long> seatIds, BigDecimal unitPrice, BigDecimal amount, List<String> ticketCodes) {}
