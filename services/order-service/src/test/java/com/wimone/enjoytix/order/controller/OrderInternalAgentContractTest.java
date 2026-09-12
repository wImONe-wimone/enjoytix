package com.wimone.enjoytix.order.controller;

import com.wimone.enjoytix.order.dto.req.AgentOrderCreateReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OrderInternalAgentContractTest {
    @Test
    void exposesDedicatedAgentCreateEndpoint() throws Exception {
        Method method = OrderInternalController.class.getDeclaredMethod("createAgentOrder", String.class, String.class, Long.class, AgentOrderCreateReqDTO.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/agent/create");
        assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream)
                .anyMatch(annotation -> annotation instanceof RequestHeader header && header.value().equals("X-Agent-Service")))
                .isTrue();
    }

    @Test
    void agentRequestCannotSupplyUserId() {
        assertThat(Arrays.stream(AgentOrderCreateReqDTO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("userId"))).isTrue();
        assertThat(Arrays.stream(AgentOrderCreateReqDTO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("purchase"))).isTrue();
    }
}
