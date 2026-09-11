package com.wimone.enjoytix.order.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderInternalControllerTest {

    @Test
    void checkPurchaseShouldReadUserIdFromTrustedHeader() throws NoSuchMethodException {
        Method method = OrderInternalController.class.getMethod("checkPurchase", Long.class, Long.class);

        RequestHeader requestHeader = method.getParameters()[0].getAnnotation(RequestHeader.class);

        assertNotNull(requestHeader);
        assertEquals("X-User-Id", requestHeader.value());
    }
}
