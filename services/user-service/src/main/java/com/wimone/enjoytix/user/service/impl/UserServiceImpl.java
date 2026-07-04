package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.user.common.enums.UserStatusEnum;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dto.req.UserLoginReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;
import com.wimone.enjoytix.user.repository.InMemoryUserRepository;
import com.wimone.enjoytix.user.service.UserService;
import com.wimone.enjoytix.user.toolkit.PasswordUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final InMemoryUserRepository userRepository;
    private final IdGeneratorManager idGeneratorManager;

    public UserServiceImpl(InMemoryUserRepository userRepository, IdGeneratorManager idGeneratorManager) {
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
        userDO.setIdCard(requestParam.getIdCard());
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
        UserLoginRespDTO result = new UserLoginRespDTO();
        result.setAccessToken("dev-" + userDO.getId());
        result.setUser(convert(userDO));
        return result;
    }

    @Override
    public UserRespDTO queryByUserId(Long userId) {
        return userRepository.findUserById(userId)
                .map(this::convert)
                .orElseThrow(() -> new ClientException("User does not exist"));
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
