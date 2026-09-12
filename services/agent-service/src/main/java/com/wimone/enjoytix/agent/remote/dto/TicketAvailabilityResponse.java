package com.wimone.enjoytix.agent.remote.dto;
import java.math.BigDecimal;
public record TicketAvailabilityResponse(Long showId, Long categoryId, String categoryName, BigDecimal price, Integer totalStock, Integer lockedStock, Integer soldStock, Integer availableStock, Integer seatSelectable) {}
