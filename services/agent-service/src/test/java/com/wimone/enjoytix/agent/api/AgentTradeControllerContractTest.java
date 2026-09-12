package com.wimone.enjoytix.agent.api;

import com.wimone.enjoytix.agent.trade.PurchaseConfirmationRequest;
import com.wimone.enjoytix.agent.trade.PurchaseDraftRequest;
import com.wimone.enjoytix.agent.trade.AgentOrderCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTradeControllerContractTest {
    @Test
    void exposesDraftConfirmationAndOrderPostEndpoints() {
        assertThat(Arrays.stream(AgentTradeController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> Arrays.stream(mapping.value())))
                .containsExactlyInAnyOrder(String.valueOf(new char[]{'/', 'd', 'r', 'a', 'f', 't', 's'}),
                        String.valueOf(new char[]{'/', 'c', 'o', 'n', 'f', 'i', 'r', 'm', 'a', 't', 'i', 'o', 'n', 's'}),
                        String.valueOf(new char[]{'/', 'o', 'r', 'd', 'e', 'r', 's'}));
    }

    @Test
    void writeRequestsDoNotAcceptClientUserId() throws Exception {
        assertThat(Arrays.stream(PurchaseDraftRequest.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals(String.valueOf(new char[]{'u', 's', 'e', 'r', 'I', 'd'})))).isTrue();
        assertThat(Arrays.stream(PurchaseConfirmationRequest.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals(String.valueOf(new char[]{'u', 's', 'e', 'r', 'I', 'd'})))).isTrue();
        assertThat(Arrays.stream(AgentOrderCreateRequest.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("purchase"))).isTrue();
        for (Method method : AgentTradeController.class.getDeclaredMethods()) {
            if (method.getAnnotation(PostMapping.class) == null) continue;
            assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream)
                    .noneMatch(annotation -> annotation.annotationType().equals(RequestHeader.class)))
                    .isTrue();
            assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream)
                    .filter(annotation -> annotation instanceof RequestBody)
                    .count()).isLessThanOrEqualTo(1);
        }
    }
}
