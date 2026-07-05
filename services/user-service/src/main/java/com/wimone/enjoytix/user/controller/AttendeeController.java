package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.AttendeeCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.AttendeeUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.AttendeeRespDTO;
import com.wimone.enjoytix.user.service.AttendeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user/attendees")
@Tag(name = "Attendee API", description = "Real-name attendee management APIs.")
public class AttendeeController {

    private final AttendeeService attendeeService;

    public AttendeeController(AttendeeService attendeeService) {
        this.attendeeService = attendeeService;
    }

    @Operation(summary = "List attendees", description = "List all attendees owned by the current user.")
    @GetMapping
    public Result<List<AttendeeRespDTO>> list(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(attendeeService.list(userId));
    }

    @OperationLog("attendee-create")
    @Operation(summary = "Create attendee", description = "Create a real-name attendee for the current user.")
    @PostMapping
    public Result<AttendeeRespDTO> create(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody AttendeeCreateReqDTO requestParam) {
        return Results.success(attendeeService.create(userId, requestParam));
    }

    @OperationLog("attendee-update")
    @Operation(summary = "Update attendee", description = "Update a real-name attendee owned by the current user.")
    @PutMapping
    public Result<AttendeeRespDTO> update(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody AttendeeUpdateReqDTO requestParam) {
        return Results.success(attendeeService.update(userId, requestParam));
    }

    @OperationLog("attendee-delete")
    @Operation(summary = "Delete attendee", description = "Delete a real-name attendee owned by the current user.")
    @DeleteMapping("/{attendeeId}")
    public Result<Boolean> delete(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Attendee id.", required = true)
            @PathVariable Long attendeeId) {
        return Results.success(attendeeService.delete(userId, attendeeId));
    }
}
