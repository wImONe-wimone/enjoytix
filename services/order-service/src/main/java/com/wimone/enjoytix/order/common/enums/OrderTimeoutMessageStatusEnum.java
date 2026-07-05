package com.wimone.enjoytix.order.common.enums;

public enum OrderTimeoutMessageStatusEnum {

    SENT,
    CONSUMING,
    SUCCESS,
    RETRYING,
    SEND_FAILED,
    DEAD_LETTER
}
