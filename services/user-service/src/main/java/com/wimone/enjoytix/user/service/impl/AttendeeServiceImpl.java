package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.user.common.enums.CertificateTypeEnum;
import com.wimone.enjoytix.user.common.enums.UserStatusEnum;
import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dto.req.AttendeeCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.AttendeeUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.AttendeeRespDTO;
import com.wimone.enjoytix.user.repository.UserRepository;
import com.wimone.enjoytix.user.service.AttendeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AttendeeServiceImpl implements AttendeeService {

    private final UserRepository userRepository;
    private final IdGeneratorManager idGeneratorManager;

    public AttendeeServiceImpl(UserRepository userRepository, IdGeneratorManager idGeneratorManager) {
        this.userRepository = userRepository;
        this.idGeneratorManager = idGeneratorManager;
    }

    @Override
    public AttendeeRespDTO create(Long userId, AttendeeCreateReqDTO requestParam) {
        ensureUserExists(userId);
        AttendeeDO attendeeDO = new AttendeeDO();
        attendeeDO.setId(idGeneratorManager.nextId());
        attendeeDO.setUserId(userId);
        fill(attendeeDO, requestParam);
        attendeeDO.setStatus(UserStatusEnum.ENABLED.code());
        attendeeDO.setCreateTime(LocalDateTime.now());
        attendeeDO.setUpdateTime(LocalDateTime.now());
        attendeeDO.setDelFlag(0);
        saveWithDefault(attendeeDO);
        return convert(attendeeDO);
    }

    @Override
    public AttendeeRespDTO update(Long userId, AttendeeUpdateReqDTO requestParam) {
        AttendeeDO attendeeDO = userRepository.findAttendee(requestParam.getAttendeeId())
                .orElseThrow(() -> new ClientException("Attendee does not exist"));
        if (!userId.equals(attendeeDO.getUserId())) {
            throw new ClientException("Attendee does not belong to current user");
        }
        fill(attendeeDO, requestParam);
        attendeeDO.setUpdateTime(LocalDateTime.now());
        saveWithDefault(attendeeDO);
        return convert(attendeeDO);
    }

    @Override
    public Boolean delete(Long userId, Long attendeeId) {
        AttendeeDO attendeeDO = userRepository.findAttendee(attendeeId)
                .orElseThrow(() -> new ClientException("Attendee does not exist"));
        if (!userId.equals(attendeeDO.getUserId())) {
            throw new ClientException("Attendee does not belong to current user");
        }
        attendeeDO.setDelFlag(1);
        attendeeDO.setDefaultFlag(0);
        attendeeDO.setUpdateTime(LocalDateTime.now());
        userRepository.saveAttendee(attendeeDO);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(readOnly = true)
    public AttendeeRespDTO detail(Long userId, Long attendeeId) {
        return convert(findOwnedAttendee(userId, attendeeId));
    }

    @Override
    public AttendeeRespDTO setDefault(Long userId, Long attendeeId) {
        AttendeeDO attendeeDO = findOwnedAttendee(userId, attendeeId);
        attendeeDO.setDefaultFlag(1);
        attendeeDO.setUpdateTime(LocalDateTime.now());
        saveWithDefault(attendeeDO);
        return convert(attendeeDO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendeeRespDTO> list(Long userId) {
        ensureUserExists(userId);
        return userRepository.listAttendees(userId).stream().map(this::convert).toList();
    }

    private void ensureUserExists(Long userId) {
        userRepository.findUserById(userId).orElseThrow(() -> new ClientException("User does not exist"));
    }

    private AttendeeDO findOwnedAttendee(Long userId, Long attendeeId) {
        AttendeeDO attendeeDO = userRepository.findAttendee(attendeeId)
                .orElseThrow(() -> new ClientException("Attendee does not exist"));
        if (!userId.equals(attendeeDO.getUserId())) {
            throw new ClientException("Attendee does not belong to current user");
        }
        return attendeeDO;
    }

    private void saveWithDefault(AttendeeDO attendeeDO) {
        attendeeDO.setDefaultFlag(Integer.valueOf(1).equals(attendeeDO.getDefaultFlag()) ? 1 : 0);
        if (Integer.valueOf(1).equals(attendeeDO.getDefaultFlag())) {
            userRepository.clearDefaultAttendee(attendeeDO.getUserId(), attendeeDO.getId());
        }
        userRepository.saveAttendee(attendeeDO);
    }

    private void fill(AttendeeDO attendeeDO, AttendeeCreateReqDTO requestParam) {
        validateCertificateType(requestParam.getCertificateType());
        attendeeDO.setRealName(requestParam.getRealName());
        attendeeDO.setCertificateType(requestParam.getCertificateType());
        attendeeDO.setCertificateNo(requestParam.getCertificateNo());
        attendeeDO.setMobile(requestParam.getMobile());
        attendeeDO.setDefaultFlag(requestParam.getDefaultFlag() == null ? 0 : requestParam.getDefaultFlag());
    }

    private void validateCertificateType(String certificateType) {
        for (CertificateTypeEnum each : CertificateTypeEnum.values()) {
            if (each.code().equals(certificateType)) {
                return;
            }
        }
        throw new ClientException("Unsupported certificate type");
    }

    private AttendeeRespDTO convert(AttendeeDO attendeeDO) {
        AttendeeRespDTO result = new AttendeeRespDTO();
        result.setAttendeeId(attendeeDO.getId());
        result.setUserId(attendeeDO.getUserId());
        result.setRealName(attendeeDO.getRealName());
        result.setCertificateType(attendeeDO.getCertificateType());
        result.setCertificateNo(attendeeDO.getCertificateNo());
        result.setMobile(attendeeDO.getMobile());
        result.setDefaultFlag(attendeeDO.getDefaultFlag());
        result.setStatus(attendeeDO.getStatus());
        return result;
    }
}
