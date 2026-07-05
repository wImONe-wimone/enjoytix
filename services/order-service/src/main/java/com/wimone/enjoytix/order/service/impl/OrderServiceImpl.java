package com.wimone.enjoytix.order.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.order.common.enums.OrderStatusEnum;
import com.wimone.enjoytix.order.dao.entity.OrderDO;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import com.wimone.enjoytix.order.dao.entity.OrderStatusLogDO;
import com.wimone.enjoytix.order.dto.req.OrderCancelReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderCreateReqDTO;
import com.wimone.enjoytix.order.dto.req.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.order.dto.resp.OrderCreateRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderDetailRespDTO;
import com.wimone.enjoytix.order.dto.resp.OrderItemRespDTO;
import com.wimone.enjoytix.order.message.NoopOrderTimeoutMessageSender;
import com.wimone.enjoytix.order.message.OrderTimeoutMessage;
import com.wimone.enjoytix.order.message.OrderTimeoutMessageSender;
import com.wimone.enjoytix.order.remote.TicketRemoteService;
import com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketIssueReqDTO;
import com.wimone.enjoytix.order.remote.dto.TicketIssueRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketLockReqDTO;
import com.wimone.enjoytix.order.remote.dto.TicketLockRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketReleaseReqDTO;
import com.wimone.enjoytix.order.repository.InMemoryOrderRepository;
import com.wimone.enjoytix.order.service.OrderService;
import com.wimone.enjoytix.order.service.OrderTimeoutCloseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService, OrderTimeoutCloseService {

    private final InMemoryOrderRepository orderRepository;
    private final TicketRemoteService ticketRemoteService;
    private final IdGeneratorManager idGeneratorManager;
    private final OrderTimeoutMessageSender orderTimeoutMessageSender;

    @Autowired
    public OrderServiceImpl(
            InMemoryOrderRepository orderRepository,
            TicketRemoteService ticketRemoteService,
            IdGeneratorManager idGeneratorManager,
            OrderTimeoutMessageSender orderTimeoutMessageSender) {
        this.orderRepository = orderRepository;
        this.ticketRemoteService = ticketRemoteService;
        this.idGeneratorManager = idGeneratorManager;
        this.orderTimeoutMessageSender = orderTimeoutMessageSender;
    }

    public OrderServiceImpl(
            InMemoryOrderRepository orderRepository,
            TicketRemoteService ticketRemoteService,
            IdGeneratorManager idGeneratorManager) {
        this(orderRepository, ticketRemoteService, idGeneratorManager, new NoopOrderTimeoutMessageSender());
    }

    @Override
    public synchronized OrderCreateRespDTO create(Long userId, OrderCreateReqDTO requestParam) {
        closeExpiredOrders();
        TicketAvailabilityRespDTO availability = findAvailability(requestParam.getShowId(), requestParam.getCategoryId());
        int quantity = requestParam.getSeatIds().isEmpty() ? normalizeQuantity(requestParam.getQuantity()) : requestParam.getSeatIds().size();
        TicketLockRespDTO ticketLock = callTicketLock(userId, new TicketLockReqDTO(
                requestParam.getShowId(),
                requestParam.getCategoryId(),
                quantity,
                requestParam.getSeatIds()
        ));
        BigDecimal totalAmount = availability.price().multiply(BigDecimal.valueOf(quantity));
        Long orderId = idGeneratorManager.nextId();
        OrderDO orderDO = new OrderDO();
        orderDO.setId(orderId);
        orderDO.setOrderSn("EO" + orderId);
        orderDO.setUserId(userId);
        orderDO.setShowId(requestParam.getShowId());
        orderDO.setLockId(ticketLock.lockId());
        orderDO.setTotalAmount(totalAmount);
        orderDO.setStatus(OrderStatusEnum.PENDING_PAYMENT.name());
        orderDO.setPayExpireTime(ticketLock.expireTime());
        orderDO.setCreateTime(LocalDateTime.now());
        orderDO.setUpdateTime(LocalDateTime.now());
        orderDO.setDelFlag(0);
        orderRepository.saveOrder(orderDO);

        OrderItemDO itemDO = new OrderItemDO();
        itemDO.setId(idGeneratorManager.nextId());
        itemDO.setOrderId(orderId);
        itemDO.setShowId(requestParam.getShowId());
        itemDO.setCategoryId(requestParam.getCategoryId());
        itemDO.setQuantity(quantity);
        itemDO.setSeatIds(ticketLock.seatIds());
        itemDO.setUnitPrice(availability.price());
        itemDO.setAmount(totalAmount);
        itemDO.setTicketCodes(List.of());
        itemDO.setCreateTime(LocalDateTime.now());
        itemDO.setUpdateTime(LocalDateTime.now());
        itemDO.setDelFlag(0);
        orderRepository.saveItem(itemDO);
        recordStatus(orderId, null, OrderStatusEnum.PENDING_PAYMENT.name(), "create order");
        orderTimeoutMessageSender.send(new OrderTimeoutMessage(orderId, ticketLock.lockId(), orderDO.getPayExpireTime()));
        return new OrderCreateRespDTO(orderId, orderDO.getOrderSn(), ticketLock.lockId(), totalAmount, orderDO.getStatus(), orderDO.getPayExpireTime());
    }

    @Override
    public synchronized Boolean cancel(Long userId, OrderCancelReqDTO requestParam) {
        OrderDO orderDO = findOrder(requestParam.getOrderId());
        assertOwner(userId, orderDO);
        if (!OrderStatusEnum.PENDING_PAYMENT.name().equals(orderDO.getStatus())) {
            throw new ClientException("Only pending orders can be canceled");
        }
        releaseTicket(orderDO);
        changeStatus(orderDO, OrderStatusEnum.CANCELED, "user cancel");
        return Boolean.TRUE;
    }

    @Override
    public synchronized OrderDetailRespDTO paySuccess(OrderPaySuccessReqDTO requestParam) {
        OrderDO orderDO = findOrder(requestParam.getOrderId());
        if (OrderStatusEnum.PAID.name().equals(orderDO.getStatus())) {
            return convert(orderDO);
        }
        if (!OrderStatusEnum.PENDING_PAYMENT.name().equals(orderDO.getStatus())) {
            throw new ClientException("Order cannot be paid in current status");
        }
        if (orderDO.getPayExpireTime().isBefore(LocalDateTime.now())) {
            closeOrder(orderDO, "payment after timeout");
            throw new ClientException("Order has expired");
        }
        TicketIssueRespDTO issueResult = callTicketIssue(orderDO.getUserId(), new TicketIssueReqDTO(orderDO.getLockId(), orderDO.getId()));
        orderRepository.listItems(orderDO.getId()).forEach(each -> {
            each.setTicketCodes(issueResult.ticketCodes());
            each.setUpdateTime(LocalDateTime.now());
            orderRepository.saveItem(each);
        });
        changeStatus(orderDO, OrderStatusEnum.PAID, "pay success");
        return convert(orderDO);
    }

    @Override
    public OrderDetailRespDTO detail(Long userId, Long orderId) {
        closeExpiredOrders();
        OrderDO orderDO = findOrder(orderId);
        assertOwner(userId, orderDO);
        return convert(orderDO);
    }

    @Override
    public List<OrderDetailRespDTO> list(Long userId) {
        closeExpiredOrders();
        return orderRepository.listOrdersByUser(userId).stream().map(this::convert).toList();
    }

    @Override
    @Scheduled(fixedDelay = 30000L)
    public synchronized void closeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        orderRepository.listOrders()
                .stream()
                .filter(each -> OrderStatusEnum.PENDING_PAYMENT.name().equals(each.getStatus()))
                .filter(each -> each.getPayExpireTime().isBefore(now))
                .forEach(each -> closeOrder(each, "payment timeout scan"));
    }

    @Override
    public synchronized boolean closeIfExpired(Long orderId, LocalDateTime expectedExpireTime, String reason) {
        return orderRepository.findOrder(orderId)
                .filter(each -> OrderStatusEnum.PENDING_PAYMENT.name().equals(each.getStatus()))
                .filter(each -> expectedExpireTime == null || expectedExpireTime.equals(each.getPayExpireTime()))
                .filter(each -> !each.getPayExpireTime().isAfter(LocalDateTime.now()))
                .map(each -> {
                    closeOrder(each, reason);
                    return Boolean.TRUE;
                })
                .orElse(Boolean.FALSE);
    }

    private void closeOrder(OrderDO orderDO, String reason) {
        releaseTicket(orderDO);
        changeStatus(orderDO, OrderStatusEnum.CLOSED, reason);
    }

    private TicketAvailabilityRespDTO findAvailability(Long showId, Long categoryId) {
        var result = ticketRemoteService.availability(showId);
        if (!result.isSuccess()) {
            throw new RemoteException("Query ticket availability failed: " + result.getMessage());
        }
        return result.getData()
                .stream()
                .filter(each -> categoryId.equals(each.categoryId()))
                .findFirst()
                .orElseThrow(() -> new ClientException("Ticket category does not exist"));
    }

    private TicketLockRespDTO callTicketLock(Long userId, TicketLockReqDTO requestParam) {
        var result = ticketRemoteService.lock(userId, requestParam);
        if (!result.isSuccess()) {
            throw new RemoteException("Lock ticket failed: " + result.getMessage());
        }
        return result.getData();
    }

    private void releaseTicket(OrderDO orderDO) {
        var result = ticketRemoteService.release(orderDO.getUserId(), new TicketReleaseReqDTO(orderDO.getLockId()));
        if (!result.isSuccess()) {
            throw new RemoteException("Release ticket lock failed: " + result.getMessage());
        }
    }

    private TicketIssueRespDTO callTicketIssue(Long userId, TicketIssueReqDTO requestParam) {
        var result = ticketRemoteService.issue(userId, requestParam);
        if (!result.isSuccess()) {
            throw new RemoteException("Issue ticket failed: " + result.getMessage());
        }
        return result.getData();
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        if (quantity > 6) {
            throw new ClientException("Single order quantity cannot exceed 6");
        }
        return quantity;
    }

    private OrderDO findOrder(Long orderId) {
        return orderRepository.findOrder(orderId).orElseThrow(() -> new ClientException("Order does not exist"));
    }

    private void assertOwner(Long userId, OrderDO orderDO) {
        if (!orderDO.getUserId().equals(userId)) {
            throw new ClientException("Order does not belong to current user");
        }
    }

    private void changeStatus(OrderDO orderDO, OrderStatusEnum targetStatus, String reason) {
        String fromStatus = orderDO.getStatus();
        orderDO.setStatus(targetStatus.name());
        orderDO.setUpdateTime(LocalDateTime.now());
        orderRepository.saveOrder(orderDO);
        recordStatus(orderDO.getId(), fromStatus, targetStatus.name(), reason);
    }

    private void recordStatus(Long orderId, String fromStatus, String toStatus, String reason) {
        OrderStatusLogDO logDO = new OrderStatusLogDO();
        logDO.setId(idGeneratorManager.nextId());
        logDO.setOrderId(orderId);
        logDO.setFromStatus(fromStatus);
        logDO.setToStatus(toStatus);
        logDO.setReason(reason);
        logDO.setCreateTime(LocalDateTime.now());
        logDO.setUpdateTime(LocalDateTime.now());
        logDO.setDelFlag(0);
        orderRepository.saveStatusLog(logDO);
    }

    private OrderDetailRespDTO convert(OrderDO orderDO) {
        List<OrderItemRespDTO> items = orderRepository.listItems(orderDO.getId())
                .stream()
                .map(this::convertItem)
                .toList();
        return new OrderDetailRespDTO(
                orderDO.getId(),
                orderDO.getOrderSn(),
                orderDO.getUserId(),
                orderDO.getShowId(),
                orderDO.getLockId(),
                orderDO.getTotalAmount(),
                orderDO.getStatus(),
                orderDO.getPayExpireTime(),
                items
        );
    }

    private OrderItemRespDTO convertItem(OrderItemDO itemDO) {
        return new OrderItemRespDTO(
                itemDO.getId(),
                itemDO.getShowId(),
                itemDO.getCategoryId(),
                itemDO.getQuantity(),
                itemDO.getSeatIds(),
                itemDO.getUnitPrice(),
                itemDO.getAmount(),
                itemDO.getTicketCodes()
        );
    }
}
