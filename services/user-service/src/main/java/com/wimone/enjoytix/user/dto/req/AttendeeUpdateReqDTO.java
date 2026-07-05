package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Attendee update request.")
public class AttendeeUpdateReqDTO extends AttendeeCreateReqDTO {

    @NotNull
    @Schema(description = "Attendee id.", example = "1001")
    private Long attendeeId;

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }
}
