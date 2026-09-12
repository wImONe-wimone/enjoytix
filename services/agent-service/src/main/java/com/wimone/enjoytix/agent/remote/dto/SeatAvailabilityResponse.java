package com.wimone.enjoytix.agent.remote.dto;
public record SeatAvailabilityResponse(Long showId, Long categoryId, Long seatId, Long areaId, Integer rowNo, Integer columnNo, String seatNo, String status) {}
