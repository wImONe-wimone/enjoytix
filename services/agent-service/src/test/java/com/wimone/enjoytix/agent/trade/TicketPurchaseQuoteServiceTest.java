package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.SeatAvailabilityResponse;
import com.wimone.enjoytix.agent.remote.dto.TicketAvailabilityResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TicketPurchaseQuoteServiceTest {
    @Test
    void buildsQuoteFromAuthoritativePriceAndAvailableSeats() {
        TicketReadRemoteService remote = mock(TicketReadRemoteService.class);
        when(remote.availability(21L)).thenReturn(Result.success(List.of(
                new TicketAvailabilityResponse(21L, 5L, "VIP", new BigDecimal("299.00"), 10, 1, 2, 7, 1))));
        when(remote.seats(21L)).thenReturn(Result.success(List.of(
                new SeatAvailabilityResponse(21L, 5L, 101L, 1L, 1, 1, "A1", "AVAILABLE"),
                new SeatAvailabilityResponse(21L, 5L, 102L, 1L, 1, 2, "A2", "AVAILABLE"))));

        TicketPurchaseQuoteService service = new RemoteTicketPurchaseQuoteService(remote);
        PurchaseQuote quote = service.quote(new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2));

        assertThat(quote.unitPrice()).isEqualByComparingTo("299.00");
        assertThat(quote.totalAmount()).isEqualByComparingTo("598.00");
        assertThat(quote.seatIds()).containsExactly(101L, 102L);
    }

    @Test
    void rejectsSeatFromAnotherCategoryOrUnavailableSeat() {
        TicketReadRemoteService remote = mock(TicketReadRemoteService.class);
        when(remote.availability(21L)).thenReturn(Result.success(List.of(
                new TicketAvailabilityResponse(21L, 5L, "VIP", new BigDecimal("299.00"), 10, 1, 2, 7, 1))));
        when(remote.seats(21L)).thenReturn(Result.success(List.of(
                new SeatAvailabilityResponse(21L, 6L, 101L, 1L, 1, 1, "A1", "AVAILABLE"),
                new SeatAvailabilityResponse(21L, 5L, 102L, 1L, 1, 2, "A2", "SOLD"))));

        TicketPurchaseQuoteService service = new RemoteTicketPurchaseQuoteService(remote);

        assertThatThrownBy(() -> service.quote(new PurchaseDraftRequest(21L, 5L, List.of(101L, 102L), 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requested seats are not available");
    }
}
