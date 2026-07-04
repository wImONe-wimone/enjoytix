package com.wimone.enjoytix.user.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.user.common.UserConstants;
import com.wimone.enjoytix.user.dto.req.AttendeeCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.AttendeeUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.AttendeeRespDTO;
import com.wimone.enjoytix.user.service.AttendeeService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/user/attendees")
public class AttendeeController {

    private final AttendeeService attendeeService;

    public AttendeeController(AttendeeService attendeeService) {
        this.attendeeService = attendeeService;
    }

    @GetMapping
    public Result<List<AttendeeRespDTO>> list(@RequestHeader(UserConstants.USER_ID_HEADER) Long userId) {
        return Results.success(attendeeService.list(userId));
    }

    @OperationLog("attendee-create")
    @PostMapping
    public Result<AttendeeRespDTO> create(
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody AttendeeCreateReqDTO requestParam) {
        return Results.success(attendeeService.create(userId, requestParam));
    }

    @OperationLog("attendee-update")
    @PutMapping
    public Result<AttendeeRespDTO> update(
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody AttendeeUpdateReqDTO requestParam) {
        return Results.success(attendeeService.update(userId, requestParam));
    }

    @OperationLog("attendee-delete")
    @DeleteMapping("/{attendeeId}")
    public Result<Boolean> delete(
            @RequestHeader(UserConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long attendeeId) {
        return Results.success(attendeeService.delete(userId, attendeeId));
    }
}
