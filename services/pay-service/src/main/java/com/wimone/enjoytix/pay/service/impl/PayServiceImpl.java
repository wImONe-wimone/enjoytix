package com.wimone.enjoytix.pay.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.pay.common.enums.PayStatusEnum;
import com.wimone.enjoytix.pay.common.enums.RefundStatusEnum;
import com.wimone.enjoytix.pay.dao.entity.PayDO;
import com.wimone.enjoytix.pay.dao.entity.RefundDO;
import com.wimone.enjoytix.pay.dto.req.MockPayReqDTO;
import com.wimone.enjoytix.pay.dto.req.PayCreateReqDTO;
import com.wimone.enjoytix.pay.dto.req.RefundApplyReqDTO;
import com.wimone.enjoytix.pay.dto.req.RefundByOrderReqDTO;
import com.wimone.enjoytix.pay.dto.resp.PayRespDTO;
import com.wimone.enjoytix.pay.dto.resp.RefundRespDTO;
import com.wimone.enjoytix.pay.remote.OrderRemoteService;
import com.wimone.enjoytix.pay.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.pay.remote.dto.OrderPaySuccessReqDTO;
import com.wimone.enjoytix.pay.repository.PayRepository;
import com.wimone.enjoytix.pay.service.PayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PayServiceImpl implements PayService {

    private final PayRepository payRepository;
    private final OrderRemoteService orderRemoteService;
    private final IdGeneratorManager idGeneratorManager;

    public PayServiceImpl(
            PayRepository payRepository,
            OrderRemoteService orderRemoteService,
            IdGeneratorManager idGeneratorManager) {
        this.payRepository = payRepository;
        this.orderRemoteService = orderRemoteService;
        this.idGeneratorManager = idGeneratorManager;
    }

    @Override
    public synchronized PayRespDTO create(Long userId, PayCreateReqDTO requestParam) {
        return payRepository.findPayByOrderId(requestParam.getOrderId())
                .map(this::convert)
                .orElseGet(() -> createNewPay(userId, requestParam));
    }

    @Override
    public synchronized PayRespDTO mockSuccess(Long userId, MockPayReqDTO requestParam) {
        PayDO payDO = findPay(requestParam.getPayId());
        assertOwner(userId, payDO);
        if (PayStatusEnum.SUCCESS.name().equals(payDO.getStatus())) {
            return convert(payDO);
        }
        if (!PayStatusEnum.WAITING.name().equals(payDO.getStatus())) {
            throw new ClientException("Pay order cannot be paid in current status");
        }
        var result = orderRemoteService.paySuccess(new OrderPaySuccessReqDTO(payDO.getOrderId()));
        if (!result.isSuccess()) {
            throw new RemoteException("Notify order pay success failed: " + result.getMessage());
        }
        payDO.setStatus(PayStatusEnum.SUCCESS.name());
        payDO.setPaidTime(LocalDateTime.now());
        payDO.setUpdateTime(LocalDateTime.now());
        payRepository.savePay(payDO);
        return convert(payDO);
    }

    @Override
    @Transactional(readOnly = true)
    public PayRespDTO detail(Long userId, Long payId) {
        PayDO payDO = findPay(payId);
        assertOwner(userId, payDO);
        return convert(payDO);
    }

    @Override
    public synchronized RefundRespDTO refund(Long userId, RefundApplyReqDTO requestParam) {
        PayDO payDO = findPay(requestParam.getPayId());
        assertOwner(userId, payDO);
        return refundPayOrder(payDO, requestParam.getReason());
    }

    @Override
    public synchronized RefundRespDTO refundByOrder(Long userId, RefundByOrderReqDTO requestParam) {
        PayDO payDO = payRepository.findPayByOrderId(requestParam.getOrderId())
                .orElseThrow(() -> new ClientException("Pay order does not exist for order"));
        assertOwner(userId, payDO);
        return refundPayOrder(payDO, requestParam.getReason());
    }

    private RefundRespDTO refundPayOrder(PayDO payDO, String reason) {
        var existingRefund = payRepository.findRefundByOrderId(payDO.getOrderId());
        if (existingRefund.isPresent()) {
            if (!PayStatusEnum.REFUNDED.name().equals(payDO.getStatus())) {
                payDO.setStatus(PayStatusEnum.REFUNDED.name());
                payDO.setUpdateTime(LocalDateTime.now());
                payRepository.savePay(payDO);
            }
            return convert(existingRefund.get());
        }
        if (PayStatusEnum.REFUNDED.name().equals(payDO.getStatus())) {
            return createRefund(payDO, reason);
        }
        if (!PayStatusEnum.SUCCESS.name().equals(payDO.getStatus())) {
            throw new ClientException("Only successful pay orders can be refunded");
        }
        payDO.setStatus(PayStatusEnum.REFUNDED.name());
        payDO.setUpdateTime(LocalDateTime.now());
        payRepository.savePay(payDO);
        return createRefund(payDO, reason);
    }

    private RefundRespDTO createRefund(PayDO payDO, String reason) {
        RefundDO refundDO = new RefundDO();
        refundDO.setId(idGeneratorManager.nextId());
        refundDO.setPayId(payDO.getId());
        refundDO.setOrderId(payDO.getOrderId());
        refundDO.setUserId(payDO.getUserId());
        refundDO.setAmount(payDO.getAmount());
        refundDO.setStatus(RefundStatusEnum.SUCCESS.name());
        refundDO.setReason(reason);
        refundDO.setCreateTime(LocalDateTime.now());
        refundDO.setUpdateTime(LocalDateTime.now());
        refundDO.setDelFlag(0);
        payRepository.saveRefund(refundDO);
        return convert(refundDO);
    }

    private PayRespDTO createNewPay(Long userId, PayCreateReqDTO requestParam) {
        OrderDetailRespDTO order = queryOrder(userId, requestParam.getOrderId());
        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new ClientException("Only pending orders can create pay order");
        }
        PayDO payDO = new PayDO();
        payDO.setId(idGeneratorManager.nextId());
        payDO.setPaySn("EP" + payDO.getId());
        payDO.setOrderId(order.orderId());
        payDO.setUserId(userId);
        payDO.setAmount(order.totalAmount());
        payDO.setStatus(PayStatusEnum.WAITING.name());
        payDO.setCreateTime(LocalDateTime.now());
        payDO.setUpdateTime(LocalDateTime.now());
        payDO.setDelFlag(0);
        payRepository.savePay(payDO);
        return convert(payDO);
    }

    private OrderDetailRespDTO queryOrder(Long userId, Long orderId) {
        var result = orderRemoteService.detail(userId, orderId);
        if (!result.isSuccess()) {
            throw new RemoteException("Query order failed: " + result.getMessage());
        }
        return result.getData();
    }

    private PayDO findPay(Long payId) {
        return payRepository.findPay(payId).orElseThrow(() -> new ClientException("Pay order does not exist"));
    }

    private void assertOwner(Long userId, PayDO payDO) {
        if (!payDO.getUserId().equals(userId)) {
            throw new ClientException("Pay order does not belong to current user");
        }
    }

    private PayRespDTO convert(PayDO payDO) {
        return new PayRespDTO(
                payDO.getId(),
                payDO.getPaySn(),
                payDO.getOrderId(),
                payDO.getAmount(),
                payDO.getStatus(),
                "mock://pay/" + payDO.getId(),
                payDO.getPaidTime()
        );
    }

    private RefundRespDTO convert(RefundDO refundDO) {
        return new RefundRespDTO(
                refundDO.getId(),
                refundDO.getPayId(),
                refundDO.getOrderId(),
                refundDO.getAmount(),
                refundDO.getStatus(),
                refundDO.getReason()
        );
    }
}
