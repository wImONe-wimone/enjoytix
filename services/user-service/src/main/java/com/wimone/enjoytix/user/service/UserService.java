package com.wimone.enjoytix.user.service;

import com.wimone.enjoytix.user.dto.req.UserLoginReqDTO;
import com.wimone.enjoytix.user.dto.req.UserProfileUpdateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;

public interface UserService {

    UserRespDTO register(UserRegisterReqDTO requestParam);

    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    UserRespDTO queryByUserId(Long userId);

    UserRespDTO updateProfile(Long userId, UserProfileUpdateReqDTO requestParam);

    Boolean logout(Long userId, String authorization);
}
