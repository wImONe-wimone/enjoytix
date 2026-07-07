package com.wimone.enjoytix.framework.web.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebAutoConfigurationTest {

    @Test
    void serializesLongIdsAsStringsWithoutChangingPrimitiveLongCounters() throws JsonProcessingException {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new WebAutoConfiguration().enjoyTixLongToStringJacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        String json = objectMapper.writeValueAsString(new SampleResponse(
                332110931670601700L,
                List.of(332110931670601701L),
                new BigInteger("332110931670601702"),
                2L
        ));

        assertThat(json).contains("\"seatMapId\":\"332110931670601700\"");
        assertThat(json).contains("\"seatIds\":[\"332110931670601701\"]");
        assertThat(json).contains("\"externalId\":\"332110931670601702\"");
        assertThat(json).contains("\"total\":2");
    }

    private record SampleResponse(Long seatMapId, List<Long> seatIds, BigInteger externalId, long total) {
    }
}
