package com.wimone.enjoytix.user.dto.req;

import jakarta.validation.constraints.NotNull;

public class AttendeeUpdateReqDTO extends AttendeeCreateReqDTO {

    @NotNull
    private Long attendeeId;

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }
}
