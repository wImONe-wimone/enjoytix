package com.wimone.enjoytix.pay.repository;

import com.wimone.enjoytix.pay.dao.entity.PayDO;
import com.wimone.enjoytix.pay.dao.entity.RefundDO;

import java.util.Optional;

public interface PayRepository {

    void savePay(PayDO payDO);

    Optional<PayDO> findPay(Long payId);

    Optional<PayDO> findPayByOrderId(Long orderId);

    void saveRefund(RefundDO refundDO);
}
