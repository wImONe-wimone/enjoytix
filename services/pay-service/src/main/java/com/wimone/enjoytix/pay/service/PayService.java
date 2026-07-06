package com.wimone.enjoytix.pay.service;

import com.wimone.enjoytix.pay.dto.req.MockPayReqDTO;
import com.wimone.enjoytix.pay.dto.req.PayCreateReqDTO;
import com.wimone.enjoytix.pay.dto.req.RefundApplyReqDTO;
import com.wimone.enjoytix.pay.dto.req.RefundByOrderReqDTO;
import com.wimone.enjoytix.pay.dto.resp.PayRespDTO;
import com.wimone.enjoytix.pay.dto.resp.RefundRespDTO;

public interface PayService {

    PayRespDTO create(Long userId, PayCreateReqDTO requestParam);

    PayRespDTO mockSuccess(Long userId, MockPayReqDTO requestParam);

    PayRespDTO detail(Long userId, Long payId);

    RefundRespDTO refund(Long userId, RefundApplyReqDTO requestParam);

    RefundRespDTO refundByOrder(Long userId, RefundByOrderReqDTO requestParam);
}
