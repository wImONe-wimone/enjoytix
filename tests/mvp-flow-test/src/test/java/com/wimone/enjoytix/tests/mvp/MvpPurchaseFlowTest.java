package com.wimone.enjoytix.tests.mvp;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator;
import com.wimone.enjoytix.order.dto.req.OrderCreateReqDTO;
import com.wimone.enjoytix.order.dto.resp.OrderCreateRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderDetailRespDTO;
import com.wimone.enjoytix.order.remote.TicketRemoteService;
import com.wimone.enjoytix.order.repository.InMemoryOrderRepository;
import com.wimone.enjoytix.order.service.impl.OrderServiceImpl;
import com.wimone.enjoytix.pay.dto.req.MockPayReqDTO;
import com.wimone.enjoytix.pay.dto.req.PayCreateReqDTO;
import com.wimone.enjoytix.pay.dto.resp.PayRespDTO;
import com.wimone.enjoytix.pay.remote.OrderRemoteService;
import com.wimone.enjoytix.pay.remote.dto.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.pay.repository.InMemoryPayRepository;
import com.wimone.enjoytix.pay.service.impl.PayServiceImpl;
import com.wimone.enjoytix.ticket.common.enums.SeatStockStatusEnum;
import com.wimone.enjoytix.ticket.dto.req.TicketIssueReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketLockReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketReleaseReqDTO;
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketIssueRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;
import com.wimone.enjoytix.ticket.repository.InMemoryTicketRepository;
import com.wimone.enjoytix.ticket.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpPurchaseFlowTest {

    @Test
    void shouldLockSeatsCreateOrderPayAndIssueTickets() {
        TestFixture fixture = newFixture(15);

        Long userId = 1L;
        OrderCreateReqDTO createOrder = new OrderCreateReqDTO();
        createOrder.setShowId(2001L);
        createOrder.setCategoryId(3001L);
        createOrder.setSeatIds(List.of(400101L, 400102L));

        OrderCreateRespDTO order = fixture.orderService().create(userId, createOrder);
        assertEquals("PENDING_PAYMENT", order.status());

        PayCreateReqDTO createPay = new PayCreateReqDTO();
        createPay.setOrderId(order.orderId());
        PayRespDTO pay = fixture.payService().create(userId, createPay);
        assertEquals("WAITING", pay.status());

        MockPayReqDTO mockPay = new MockPayReqDTO();
        mockPay.setPayId(pay.payId());
        PayRespDTO paid = fixture.payService().mockSuccess(userId, mockPay);
        assertEquals("SUCCESS", paid.status());

        OrderDetailRespDTO paidOrder = fixture.orderService().detail(userId, order.orderId());
        assertEquals("PAID", paidOrder.status());
        assertEquals(2, paidOrder.items().get(0).ticketCodes().size());

        List<SeatAvailabilityRespDTO> seats = fixture.ticketService().seats(2001L);
        assertTrue(seats.stream()
                .filter(each -> List.of(400101L, 400102L).contains(each.seatId()))
                .allMatch(each -> SeatStockStatusEnum.SOLD.name().equals(each.status())));
    }

    @Test
    void shouldCloseExpiredOrderAndReleaseLockedSeats() {
        TestFixture fixture = newFixture(0);

        Long userId = 1L;
        OrderCreateReqDTO createOrder = new OrderCreateReqDTO();
        createOrder.setShowId(2001L);
        createOrder.setCategoryId(3001L);
        createOrder.setSeatIds(List.of(400103L));

        OrderCreateRespDTO order = fixture.orderService().create(userId, createOrder);
        assertTrue(fixture.orderService().closeIfExpired(order.orderId(), order.payExpireTime(), "test timeout message"));

        OrderDetailRespDTO closedOrder = fixture.orderService().detail(userId, order.orderId());
        assertEquals("CLOSED", closedOrder.status());

        List<SeatAvailabilityRespDTO> seats = fixture.ticketService().seats(2001L);
        assertTrue(seats.stream()
                .filter(each -> List.of(400103L).contains(each.seatId()))
                .allMatch(each -> SeatStockStatusEnum.AVAILABLE.name().equals(each.status())));
    }

    private TestFixture newFixture(long lockTtlMinutes) {
        IdGeneratorManager idGeneratorManager = new IdGeneratorManager(new SnowflakeIdGenerator(17));
        InMemoryTicketRepository ticketRepository = new InMemoryTicketRepository();
        ticketRepository.initSeedData();
        TicketServiceImpl ticketService = new TicketServiceImpl(ticketRepository, idGeneratorManager, lockTtlMinutes);
        TicketRemoteService ticketRemote = new LocalTicketRemoteService(ticketService);
        OrderServiceImpl orderService = new OrderServiceImpl(new InMemoryOrderRepository(), ticketRemote, idGeneratorManager);
        OrderRemoteService orderRemote = new LocalOrderRemoteService(orderService);
        PayServiceImpl payService = new PayServiceImpl(new InMemoryPayRepository(), orderRemote, idGeneratorManager);
        return new TestFixture(ticketService, orderService, payService);
    }

    private record TestFixture(
            TicketServiceImpl ticketService,
            OrderServiceImpl orderService,
            PayServiceImpl payService
    ) {
    }

    private record LocalTicketRemoteService(TicketServiceImpl delegate) implements TicketRemoteService {

        @Override
        public Result<List<com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO>> availability(Long showId) {
            List<com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO> data = delegate.availability(showId)
                    .stream()
                    .map(this::convertAvailability)
                    .toList();
            return Result.success(data);
        }

        @Override
        public Result<com.wimone.enjoytix.order.remote.dto.TicketLockRespDTO> lock(Long userId, com.wimone.enjoytix.order.remote.dto.TicketLockReqDTO requestParam) {
            TicketLockReqDTO lockReq = new TicketLockReqDTO();
            lockReq.setShowId(requestParam.showId());
            lockReq.setCategoryId(requestParam.categoryId());
            lockReq.setQuantity(requestParam.quantity());
            lockReq.setSeatIds(requestParam.seatIds());
            TicketLockRespDTO result = delegate.lock(userId, lockReq);
            return Result.success(new com.wimone.enjoytix.order.remote.dto.TicketLockRespDTO(
                    result.lockId(),
                    result.showId(),
                    result.categoryId(),
                    result.quantity(),
                    result.seatIds(),
                    result.expireTime()
            ));
        }

        @Override
        public Result<Boolean> release(Long userId, com.wimone.enjoytix.order.remote.dto.TicketReleaseReqDTO requestParam) {
            TicketReleaseReqDTO releaseReq = new TicketReleaseReqDTO();
            releaseReq.setLockId(requestParam.lockId());
            return Result.success(delegate.release(userId, releaseReq));
        }

        @Override
        public Result<com.wimone.enjoytix.order.remote.dto.TicketIssueRespDTO> issue(Long userId, com.wimone.enjoytix.order.remote.dto.TicketIssueReqDTO requestParam) {
            TicketIssueReqDTO issueReq = new TicketIssueReqDTO();
            issueReq.setLockId(requestParam.lockId());
            issueReq.setOrderId(requestParam.orderId());
            TicketIssueRespDTO result = delegate.issue(userId, issueReq);
            return Result.success(new com.wimone.enjoytix.order.remote.dto.TicketIssueRespDTO(
                    result.lockId(),
                    result.orderId(),
                    result.ticketCodes()
            ));
        }

        private com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO convertAvailability(TicketAvailabilityRespDTO source) {
            return new com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO(
                    source.showId(),
                    source.categoryId(),
                    source.categoryName(),
                    source.price(),
                    source.totalStock(),
                    source.lockedStock(),
                    source.soldStock(),
                    source.availableStock(),
                    source.seatSelectable()
            );
        }
    }

    private record LocalOrderRemoteService(OrderServiceImpl delegate) implements OrderRemoteService {

        @Override
        public Result<com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO> detail(Long userId, Long orderId) {
            return Result.success(convert(delegate.detail(userId, orderId)));
        }

        @Override
        public Result<com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO> paySuccess(OrderPaySuccessReqDTO requestParam) {
            com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO paySuccessReq =
                    new com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO();
            paySuccessReq.setOrderId(requestParam.orderId());
            return Result.success(convert(delegate.paySuccess(paySuccessReq)));
        }

        private com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO convert(OrderDetailRespDTO source) {
            return new com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO(
                    source.orderId(),
                    source.orderSn(),
                    source.userId(),
                    source.showId(),
                    source.lockId(),
                    source.totalAmount(),
                    source.status(),
                    source.payExpireTime(),
                    source.items().stream()
                            .map(each -> new com.wimone.enjoytix.pay.remote.dto.OrderItemRespDTO(
                                    each.itemId(),
                                    each.showId(),
                                    each.categoryId(),
                                    each.quantity(),
                                    each.seatIds(),
                                    each.unitPrice(),
                                    each.amount(),
                                    each.ticketCodes()
                            ))
                            .toList()
            );
        }
    }
}
