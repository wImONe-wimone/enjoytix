package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.SeatAvailabilityResponse;
import com.wimone.enjoytix.agent.remote.dto.TicketAvailabilityResponse;
import com.wimone.enjoytix.framework.convention.result.Result;

import java.math.BigDecimal;
import java.util.List;

public class RemoteTicketPurchaseQuoteService implements TicketPurchaseQuoteService {
    private final TicketReadRemoteService ticketReadRemoteService;

    public RemoteTicketPurchaseQuoteService(TicketReadRemoteService ticketReadRemoteService) {
        this.ticketReadRemoteService = ticketReadRemoteService;
    }

    @Override
    public PurchaseQuote quote(PurchaseDraftRequest request) {
        Result<List<TicketAvailabilityResponse>> availabilityResult = ticketReadRemoteService.availability(request.showId());
        Result<List<SeatAvailabilityResponse>> seatsResult = ticketReadRemoteService.seats(request.showId());
        if (!successful(availabilityResult) || !successful(seatsResult)) {
            throw new IllegalStateException("ticket availability query failed");
        }
        TicketAvailabilityResponse category = availabilityResult.getData().stream()
                .filter(item -> request.categoryId().equals(item.categoryId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ticket category is unavailable"));
        if (category.price() == null || category.availableStock() == null || category.availableStock() < request.quantity()) {
            throw new IllegalArgumentException("ticket category is unavailable");
        }
        boolean seatsAvailable = request.seatIds().stream().allMatch(seatId -> seatsResult.getData().stream()
                .anyMatch(seat -> request.categoryId().equals(seat.categoryId())
                        && seatId.equals(seat.seatId())
                        && "AVAILABLE".equalsIgnoreCase(seat.status())));
        if (!seatsAvailable) {
            throw new IllegalArgumentException("requested seats are not available");
        }
        BigDecimal totalAmount = category.price().multiply(BigDecimal.valueOf(request.quantity()));
        return new PurchaseQuote(request.seatIds(), category.price(), totalAmount);
    }

    private boolean successful(Result<?> result) {
        return result != null && result.isSuccess() && result.getData() != null;
    }
}
