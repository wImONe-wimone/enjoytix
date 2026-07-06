package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.user.common.enums.UserStatusEnum;
import com.wimone.enjoytix.user.dao.entity.UserSessionDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dto.req.UserLoginReqDTO;
import com.wimone.enjoytix.user.dto.req.UserProfileUpdateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;
import com.wimone.enjoytix.user.repository.UserRepository;
import com.wimone.enjoytix.user.service.UserService;
import com.wimone.enjoytix.user.toolkit.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int TOKEN_EXPIRE_DAYS = 7;

    private final UserRepository userRepository;
    private final IdGeneratorManager idGeneratorManager;

    public UserServiceImpl(UserRepository userRepository, IdGeneratorManager idGeneratorManager) {
        this.userRepository = userRepository;
        this.idGeneratorManager = idGeneratorManager;
    }

    @Override
    public UserRespDTO register(UserRegisterReqDTO requestParam) {
        if (userRepository.existsUsername(requestParam.getUsername())) {
            throw new ClientException("Username already exists");
        }
        UserDO userDO = new UserDO();
        userDO.setId(idGeneratorManager.nextId());
        userDO.setUsername(requestParam.getUsername());
        userDO.setPasswordHash(PasswordUtil.hash(requestParam.getPassword()));
        userDO.setMobile(requestParam.getMobile());
        userDO.setRealName(requestParam.getRealName());
        userDO.setStatus(UserStatusEnum.ENABLED.code());
        userDO.setCreateTime(LocalDateTime.now());
        userDO.setUpdateTime(LocalDateTime.now());
        userDO.setDelFlag(0);
        userRepository.saveUser(userDO);
        return convert(userDO);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        UserDO userDO = userRepository.findUserByUsername(requestParam.getUsername())
                .orElseThrow(() -> new ClientException("Username or password is invalid"));
        if (!PasswordUtil.matches(requestParam.getPassword(), userDO.getPasswordHash())) {
            throw new ClientException("Username or password is invalid");
        }
        if (!Integer.valueOf(UserStatusEnum.ENABLED.code()).equals(userDO.getStatus())) {
            throw new ClientException("User is disabled");
        }
        String accessToken = "dev-" + userDO.getId();
        UserSessionDO sessionDO = new UserSessionDO();
        sessionDO.setId(idGeneratorManager.nextId());
        sessionDO.setUserId(userDO.getId());
        sessionDO.setAccessToken(accessToken);
        sessionDO.setValidFlag(1);
        sessionDO.setExpireTime(LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS));
        sessionDO.setCreateTime(LocalDateTime.now());
        sessionDO.setUpdateTime(LocalDateTime.now());
        sessionDO.setDelFlag(0);
        userRepository.saveSession(sessionDO);
        UserLoginRespDTO result = new UserLoginRespDTO();
        result.setAccessToken(accessToken);
        result.setUser(convert(userDO));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserRespDTO queryByUserId(Long userId) {
        return userRepository.findUserById(userId)
                .map(this::convert)
                .orElseThrow(() -> new ClientException("User does not exist"));
    }

    @Override
    public UserRespDTO updateProfile(Long userId, UserProfileUpdateReqDTO requestParam) {
        UserDO userDO = userRepository.findUserById(userId)
                .orElseThrow(() -> new ClientException("User does not exist"));
        userDO.setMobile(requestParam.getMobile());
        userDO.setRealName(requestParam.getRealName());
        userDO.setUpdateTime(LocalDateTime.now());
        userRepository.saveUser(userDO);
        return convert(userDO);
    }

    @Override
    public Boolean logout(Long userId, String authorization) {
        userRepository.findUserById(userId).orElseThrow(() -> new ClientException("User does not exist"));
        extractToken(authorization).ifPresent(token -> userRepository.findSessionByToken(token).ifPresent(sessionDO -> {
            if (!userId.equals(sessionDO.getUserId())) {
                throw new ClientException("Token does not belong to current user");
            }
            sessionDO.setValidFlag(0);
            sessionDO.setLogoutTime(LocalDateTime.now());
            sessionDO.setUpdateTime(LocalDateTime.now());
            userRepository.saveSession(sessionDO);
        }));
        userRepository.invalidateUserSessions(userId);
        return Boolean.TRUE;
    }

    private Optional<String> extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring(BEARER_PREFIX.length()));
    }

    private UserRespDTO convert(UserDO userDO) {
        UserRespDTO result = new UserRespDTO();
        result.setUserId(userDO.getId());
        result.setUsername(userDO.getUsername());
        result.setMobile(userDO.getMobile());
        result.setRealName(userDO.getRealName());
        result.setStatus(userDO.getStatus());
        return result;
    }
}
