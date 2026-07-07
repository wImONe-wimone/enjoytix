package com.wimone.enjoytix.ticket.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.ticket.common.enums.SeatStockStatusEnum;
import com.wimone.enjoytix.ticket.dto.req.TicketCategoryStockConfigReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketCategoryMappingReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketLockReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketSeatStockConfigReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketShowStockConfigInitReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketShowStockInitReqDTO;
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;
import com.wimone.enjoytix.ticket.repository.InMemoryTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketServiceImplTest {

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        InMemoryTicketRepository repository = new InMemoryTicketRepository();
        repository.initSeedData();
        AtomicLong sequence = new AtomicLong(900000L);
        ticketService = new TicketServiceImpl(repository, new IdGeneratorManager(sequence::getAndIncrement), 15);
    }

    @Test
    void initShowStockShouldCopyStocksAndSeatsToTargetShow() {
        Long targetShowId = 9001L;
        TicketShowStockInitReqDTO request = new TicketShowStockInitReqDTO(
                2001L,
                targetShowId,
                List.of(
                        new TicketCategoryMappingReqDTO(3001L, 93001L),
                        new TicketCategoryMappingReqDTO(3002L, 93002L),
                        new TicketCategoryMappingReqDTO(3003L, 93003L)
                )
        );

        assertTrue(ticketService.initShowStock(request));

        List<TicketAvailabilityRespDTO> targetAvailability = ticketService.availability(targetShowId);
        assertEquals(3, targetAvailability.size());
        assertTrue(targetAvailability.stream().anyMatch(each ->
                targetShowId.equals(each.showId())
                        && 93001L == each.categoryId()
                        && "VIP".equals(each.categoryName())
                        && each.totalStock().equals(each.availableStock())
                        && each.lockedStock() == 0
                        && each.soldStock() == 0));

        List<SeatAvailabilityRespDTO> targetSeats = ticketService.seats(targetShowId);
        assertEquals(48, targetSeats.size());
        assertTrue(targetSeats.stream().allMatch(each ->
                targetShowId.equals(each.showId())
                        && SeatStockStatusEnum.AVAILABLE.name().equals(each.status())));
        assertTrue(targetSeats.stream().anyMatch(each -> 93001L == each.categoryId()));
        assertTrue(targetSeats.stream().anyMatch(each -> 93002L == each.categoryId()));
        assertTrue(targetSeats.stream().anyMatch(each -> 93003L == each.categoryId()));

        TicketLockReqDTO lockRequest = new TicketLockReqDTO();
        lockRequest.setShowId(targetShowId);
        lockRequest.setCategoryId(93001L);
        lockRequest.setQuantity(1);

        TicketLockRespDTO lock = ticketService.lock(100L, lockRequest);

        assertEquals(targetShowId, lock.showId());
        assertEquals(93001L, lock.categoryId());
        assertEquals(1, lock.quantity());
        assertEquals(3, ticketService.availability(2001L).size());
    }

    @Test
    void initConfiguredShowStockShouldCreateConfiguredStocksAndSeats() {
        Long showId = 9101L;
        TicketShowStockConfigInitReqDTO request = new TicketShowStockConfigInitReqDTO(
                showId,
                List.of(
                        categoryConfig(94001L, "VIP", "990.00", 2, 1, List.of(
                                seatConfig(910101L, "Front", 1, 1, "A1"),
                                seatConfig(910102L, "Front", 1, 2, "A2")
                        )),
                        categoryConfig(94002L, "Standing", "199.00", 30, 0, null)
                )
        );

        assertTrue(ticketService.initConfiguredShowStock(request));

        List<TicketAvailabilityRespDTO> availability = ticketService.availability(showId);
        assertEquals(2, availability.size());
        assertTrue(availability.stream().anyMatch(each ->
                showId.equals(each.showId())
                        && 94001L == each.categoryId()
                        && "VIP".equals(each.categoryName())
                        && each.totalStock() == 2
                        && each.availableStock() == 2));
        assertTrue(availability.stream().anyMatch(each ->
                showId.equals(each.showId())
                        && 94002L == each.categoryId()
                        && "Standing".equals(each.categoryName())
                        && each.totalStock() == 30
                        && each.availableStock() == 30));

        List<SeatAvailabilityRespDTO> seats = ticketService.seats(showId);
        assertEquals(2, seats.size());
        assertTrue(seats.stream().allMatch(each -> 94001L == each.categoryId()));
        assertTrue(seats.stream().allMatch(each -> SeatStockStatusEnum.AVAILABLE.name().equals(each.status())));

        TicketLockReqDTO lockRequest = new TicketLockReqDTO();
        lockRequest.setShowId(showId);
        lockRequest.setCategoryId(94001L);
        lockRequest.setQuantity(1);
        lockRequest.setSeatIds(List.of(910101L));

        TicketLockRespDTO lock = ticketService.lock(100L, lockRequest);

        assertEquals(showId, lock.showId());
        assertEquals(94001L, lock.categoryId());
        assertEquals(List.of(910101L), lock.seatIds());
    }

    @Test
    void initConfiguredShowStockShouldRejectDuplicateSeats() {
        TicketShowStockConfigInitReqDTO request = new TicketShowStockConfigInitReqDTO(
                9102L,
                List.of(
                        categoryConfig(94011L, "VIP", "990.00", 1, 1, List.of(seatConfig(910201L, "Front", 1, 1, "A1"))),
                        categoryConfig(94012L, "A Zone", "590.00", 1, 1, List.of(seatConfig(910201L, "Front", 1, 1, "A1")))
                )
        );

        assertThrows(ClientException.class, () -> ticketService.initConfiguredShowStock(request));
    }

    @Test
    void initConfiguredShowStockShouldKeepPreLockedSeatsUnavailable() {
        Long showId = 9103L;
        TicketShowStockConfigInitReqDTO request = new TicketShowStockConfigInitReqDTO(
                showId,
                List.of(categoryConfig(94021L, "VIP", "990.00", 2, 1, List.of(
                        seatConfig(910301L, "Front", 1, 1, "A1", true),
                        seatConfig(910302L, "Front", 1, 2, "A2")
                )))
        );

        assertTrue(ticketService.initConfiguredShowStock(request));

        TicketAvailabilityRespDTO availability = ticketService.availability(showId).get(0);
        assertEquals(2, availability.totalStock());
        assertEquals(1, availability.lockedStock());
        assertEquals(1, availability.availableStock());

        List<SeatAvailabilityRespDTO> seats = ticketService.seats(showId);
        assertTrue(seats.stream().anyMatch(each ->
                910301L == each.seatId()
                        && SeatStockStatusEnum.LOCKED.name().equals(each.status())));
        assertTrue(seats.stream().anyMatch(each ->
                910302L == each.seatId()
                        && SeatStockStatusEnum.AVAILABLE.name().equals(each.status())));
    }

    @Test
    void initShowStockShouldCopyConfiguredSeatLocksAsUnavailable() {
        Long sourceShowId = 9104L;
        Long targetShowId = 9105L;
        TicketShowStockConfigInitReqDTO sourceRequest = new TicketShowStockConfigInitReqDTO(
                sourceShowId,
                List.of(categoryConfig(94031L, "VIP", "990.00", 2, 1, List.of(
                        seatConfig(910401L, "Front", 1, 1, "A1", true),
                        seatConfig(910402L, "Front", 1, 2, "A2")
                )))
        );
        assertTrue(ticketService.initConfiguredShowStock(sourceRequest));

        assertTrue(ticketService.initShowStock(new TicketShowStockInitReqDTO(
                sourceShowId,
                targetShowId,
                List.of(new TicketCategoryMappingReqDTO(94031L, 95031L))
        )));

        TicketAvailabilityRespDTO availability = ticketService.availability(targetShowId).get(0);
        assertEquals(2, availability.totalStock());
        assertEquals(1, availability.lockedStock());
        assertEquals(1, availability.availableStock());
        List<SeatAvailabilityRespDTO> seats = ticketService.seats(targetShowId);
        assertTrue(seats.stream().anyMatch(each ->
                910401L == each.seatId()
                        && 95031L == each.categoryId()
                        && SeatStockStatusEnum.LOCKED.name().equals(each.status())));
        assertTrue(seats.stream().anyMatch(each ->
                910402L == each.seatId()
                        && 95031L == each.categoryId()
                        && SeatStockStatusEnum.AVAILABLE.name().equals(each.status())));
    }

    private TicketCategoryStockConfigReqDTO categoryConfig(
            Long categoryId,
            String categoryName,
            String price,
            Integer totalStock,
            Integer seatSelectable,
            List<TicketSeatStockConfigReqDTO> seats) {
        return new TicketCategoryStockConfigReqDTO(
                categoryId,
                categoryName,
                new BigDecimal(price),
                totalStock,
                seatSelectable,
                seats
        );
    }

    private TicketSeatStockConfigReqDTO seatConfig(Long seatId, String areaName, Integer rowNo, Integer columnNo, String seatNo) {
        return seatConfig(seatId, areaName, rowNo, columnNo, seatNo, false);
    }

    private TicketSeatStockConfigReqDTO seatConfig(Long seatId, String areaName, Integer rowNo, Integer columnNo, String seatNo, boolean locked) {
        return new TicketSeatStockConfigReqDTO(seatId, areaName, rowNo, columnNo, seatNo, locked);
    }
}
