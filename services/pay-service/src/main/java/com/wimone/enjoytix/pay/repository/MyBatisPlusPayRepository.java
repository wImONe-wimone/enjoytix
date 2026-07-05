package com.wimone.enjoytix.pay.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.pay.dao.entity.PayDO;
import com.wimone.enjoytix.pay.dao.entity.RefundDO;
import com.wimone.enjoytix.pay.dao.mapper.PayMapper;
import com.wimone.enjoytix.pay.dao.mapper.RefundMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusPayRepository implements PayRepository {

    private final PayMapper payMapper;
    private final RefundMapper refundMapper;

    public MyBatisPlusPayRepository(PayMapper payMapper, RefundMapper refundMapper) {
        this.payMapper = payMapper;
        this.refundMapper = refundMapper;
    }

    @Override
    public void savePay(PayDO payDO) {
        if (payMapper.selectById(payDO.getId()) == null) {
            payMapper.insert(payDO);
            return;
        }
        payMapper.updateById(payDO);
    }

    @Override
    public Optional<PayDO> findPay(Long payId) {
        return Optional.ofNullable(payMapper.selectById(payId));
    }

    @Override
    public Optional<PayDO> findPayByOrderId(Long orderId) {
        return Optional.ofNullable(payMapper.selectOne(Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderId, orderId)
                .last("LIMIT 1")));
    }

    @Override
    public void saveRefund(RefundDO refundDO) {
        if (refundMapper.selectById(refundDO.getId()) == null) {
            refundMapper.insert(refundDO);
            return;
        }
        refundMapper.updateById(refundDO);
    }
}
