package com.wimone.enjoytix.order.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Order item response.")
public record OrderItemRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Order item id.", type = "string", example = "332110931670601701")
        Long itemId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Show session id.", type = "string", example = "2001")
        Long showId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Ticket category id.", type = "string", example = "3001")
        Long categoryId,
        @Schema(description = "Ticket quantity.", example = "1")
        Integer quantity,
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        @Schema(description = "Selected seat ids. Serialized as strings to preserve large id precision.", example = "[\"400101\"]")
        List<Long> seatIds,
        @Schema(description = "Unit ticket price.", example = "1280.00")
        BigDecimal unitPrice,
        @Schema(description = "Item amount.", example = "1280.00")
        BigDecimal amount,
        @Schema(description = "Issued electronic ticket codes.")
        List<String> ticketCodes) {
}
