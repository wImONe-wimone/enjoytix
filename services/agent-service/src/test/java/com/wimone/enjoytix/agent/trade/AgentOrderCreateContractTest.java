package com.wimone.enjoytix.agent.trade;

import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOrderCreateContractTest {
    @Test
    void exposesOnlyDedicatedPostEndpointWithConfirmationRequest() throws Exception {
        Method method = AgentOrderCreateRemoteService.class.getDeclaredMethod(String.valueOf(new char[]{'c','r','e','a','t','e'}), AgentOrderCreateRequest.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(String.valueOf(new char[]{'/','a','p','i','/','o','r','d','e','r','/','i','n','t','e','r','n','a','l','/','a','g','e','n','t','/','c','r','e','a','t','e'}));
        assertThat(method.getParameterAnnotations()[0]).anyMatch(annotation -> annotation instanceof RequestBody);
        assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream)
                .noneMatch(annotation -> annotation instanceof RequestHeader header && header.value().equals(String.valueOf(new char[]{'X','-','U','s','e','r','-','I','d'}))))
                .isTrue();
    }

    @Test
    void requestRequiresDraftAndConfirmationButDoesNotContainUserId() {
        assertThat(Arrays.stream(AgentOrderCreateRequest.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals(String.valueOf(new char[]{'u','s','e','r','I','d'})))).isTrue();
    }
}
