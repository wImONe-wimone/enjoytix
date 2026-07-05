package com.wimone.enjoytix.pay.repository;

import com.wimone.enjoytix.pay.dao.entity.PayDO;
import com.wimone.enjoytix.pay.dao.entity.RefundDO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryPayRepository implements PayRepository {

    private final Map<Long, PayDO> payOrders = new ConcurrentHashMap<>();
    private final Map<Long, Long> payIdByOrderId = new ConcurrentHashMap<>();
    private final Map<Long, RefundDO> refunds = new ConcurrentHashMap<>();

    @Override
    public void savePay(PayDO payDO) {
        payOrders.put(payDO.getId(), payDO);
        payIdByOrderId.put(payDO.getOrderId(), payDO.getId());
    }

    @Override
    public Optional<PayDO> findPay(Long payId) {
        return Optional.ofNullable(payOrders.get(payId));
    }

    @Override
    public Optional<PayDO> findPayByOrderId(Long orderId) {
        Long payId = payIdByOrderId.get(orderId);
        return payId == null ? Optional.empty() : Optional.ofNullable(payOrders.get(payId));
    }

    @Override
    public void saveRefund(RefundDO refundDO) {
        refunds.put(refundDO.getId(), refundDO);
    }
}
