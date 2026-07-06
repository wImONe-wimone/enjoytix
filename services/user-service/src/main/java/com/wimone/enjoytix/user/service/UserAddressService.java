package com.wimone.enjoytix.user.service;

import com.wimone.enjoytix.user.dto.req.UserAddressCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserAddressUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserAddressRespDTO;

import java.util.List;

public interface UserAddressService {

    UserAddressRespDTO create(Long userId, UserAddressCreateReqDTO requestParam);

    UserAddressRespDTO update(Long userId, UserAddressUpdateReqDTO requestParam);

    UserAddressRespDTO setDefault(Long userId, Long addressId);

    Boolean delete(Long userId, Long addressId);

    UserAddressRespDTO detail(Long userId, Long addressId);

    List<UserAddressRespDTO> list(Long userId);
}
