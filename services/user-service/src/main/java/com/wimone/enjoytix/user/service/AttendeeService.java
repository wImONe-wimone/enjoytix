package com.wimone.enjoytix.user.service;

import com.wimone.enjoytix.user.dto.req.AttendeeCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.AttendeeUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.AttendeeRespDTO;

import java.util.List;

public interface AttendeeService {

    AttendeeRespDTO create(Long userId, AttendeeCreateReqDTO requestParam);

    AttendeeRespDTO update(Long userId, AttendeeUpdateReqDTO requestParam);

    Boolean delete(Long userId, Long attendeeId);

    List<AttendeeRespDTO> list(Long userId);
}
