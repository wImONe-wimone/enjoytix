package com.wimone.enjoytix.order.dto.resp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDetailRespDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesLargeIdsAsStrings() throws JsonProcessingException {
        Long orderId = 332110931670601700L;
        Long itemId = 332110931670601701L;
        Long seatId = 332110931670601702L;
        OrderDetailRespDTO response = new OrderDetailRespDTO(
                orderId,
                "EO" + orderId,
                332110931670601703L,
                2001L,
                332110931670601704L,
                new BigDecimal("1280.00"),
                "PENDING_PAYMENT",
                null,
                List.of(new OrderItemRespDTO(
                        itemId,
                        2001L,
                        3001L,
                        1,
                        List.of(seatId),
                        new BigDecimal("1280.00"),
                        new BigDecimal("1280.00"),
                        List.of()))
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"orderId\":\"332110931670601700\"");
        assertThat(json).contains("\"userId\":\"332110931670601703\"");
        assertThat(json).contains("\"lockId\":\"332110931670601704\"");
        assertThat(json).contains("\"itemId\":\"332110931670601701\"");
        assertThat(json).contains("\"seatIds\":[\"332110931670601702\"]");
    }
}
