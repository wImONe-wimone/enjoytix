package com.wimone.enjoytix.comment.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cursor page response.")
public record CursorPageRespDTO<T>(
        @Schema(description = "Next cursor. Empty when no more records.")
        String nextCursor,
        @Schema(description = "Whether more records are available.")
        Boolean hasMore,
        @Schema(description = "Page records.")
        List<T> records) {
}
