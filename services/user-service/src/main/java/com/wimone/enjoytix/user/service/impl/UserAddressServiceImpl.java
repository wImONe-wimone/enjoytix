package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.user.dao.entity.UserAddressDO;
import com.wimone.enjoytix.user.dto.req.UserAddressCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserAddressUpdateReqDTO;
import com.wimone.enjoytix.user.dto.resp.UserAddressRespDTO;
import com.wimone.enjoytix.user.repository.UserRepository;
import com.wimone.enjoytix.user.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserAddressServiceImpl implements UserAddressService {

    private final UserRepository userRepository;
    private final IdGeneratorManager idGeneratorManager;

    public UserAddressServiceImpl(UserRepository userRepository, IdGeneratorManager idGeneratorManager) {
        this.userRepository = userRepository;
        this.idGeneratorManager = idGeneratorManager;
    }

    @Override
    public UserAddressRespDTO create(Long userId, UserAddressCreateReqDTO requestParam) {
        ensureUserExists(userId);
        UserAddressDO addressDO = new UserAddressDO();
        addressDO.setId(idGeneratorManager.nextId());
        addressDO.setUserId(userId);
        fill(addressDO, requestParam);
        addressDO.setCreateTime(LocalDateTime.now());
        addressDO.setUpdateTime(LocalDateTime.now());
        addressDO.setDelFlag(0);
        saveWithDefault(addressDO);
        return convert(addressDO);
    }

    @Override
    public UserAddressRespDTO update(Long userId, UserAddressUpdateReqDTO requestParam) {
        UserAddressDO addressDO = findOwnedAddress(userId, requestParam.getAddressId());
        fill(addressDO, requestParam);
        addressDO.setUpdateTime(LocalDateTime.now());
        saveWithDefault(addressDO);
        return convert(addressDO);
    }

    @Override
    public UserAddressRespDTO setDefault(Long userId, Long addressId) {
        UserAddressDO addressDO = findOwnedAddress(userId, addressId);
        addressDO.setDefaultFlag(1);
        addressDO.setUpdateTime(LocalDateTime.now());
        saveWithDefault(addressDO);
        return convert(addressDO);
    }

    @Override
    public Boolean delete(Long userId, Long addressId) {
        UserAddressDO addressDO = findOwnedAddress(userId, addressId);
        addressDO.setDelFlag(1);
        addressDO.setDefaultFlag(0);
        addressDO.setUpdateTime(LocalDateTime.now());
        userRepository.saveAddress(addressDO);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddressRespDTO detail(Long userId, Long addressId) {
        return convert(findOwnedAddress(userId, addressId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressRespDTO> list(Long userId) {
        ensureUserExists(userId);
        return userRepository.listAddresses(userId).stream().map(this::convert).toList();
    }

    private void ensureUserExists(Long userId) {
        userRepository.findUserById(userId).orElseThrow(() -> new ClientException("User does not exist"));
    }

    private UserAddressDO findOwnedAddress(Long userId, Long addressId) {
        UserAddressDO addressDO = userRepository.findAddress(addressId)
                .orElseThrow(() -> new ClientException("Address does not exist"));
        if (!userId.equals(addressDO.getUserId())) {
            throw new ClientException("Address does not belong to current user");
        }
        return addressDO;
    }

    private void saveWithDefault(UserAddressDO addressDO) {
        addressDO.setDefaultFlag(Integer.valueOf(1).equals(addressDO.getDefaultFlag()) ? 1 : 0);
        if (Integer.valueOf(1).equals(addressDO.getDefaultFlag())) {
            userRepository.clearDefaultAddress(addressDO.getUserId(), addressDO.getId());
        }
        userRepository.saveAddress(addressDO);
    }

    private void fill(UserAddressDO addressDO, UserAddressCreateReqDTO requestParam) {
        addressDO.setReceiverName(requestParam.getReceiverName());
        addressDO.setReceiverMobile(requestParam.getReceiverMobile());
        addressDO.setProvince(requestParam.getProvince());
        addressDO.setCity(requestParam.getCity());
        addressDO.setDistrict(requestParam.getDistrict());
        addressDO.setDetailAddress(requestParam.getDetailAddress());
        addressDO.setPostalCode(requestParam.getPostalCode());
        addressDO.setDefaultFlag(requestParam.getDefaultFlag() == null ? 0 : requestParam.getDefaultFlag());
    }

    private UserAddressRespDTO convert(UserAddressDO addressDO) {
        UserAddressRespDTO result = new UserAddressRespDTO();
        result.setAddressId(addressDO.getId());
        result.setUserId(addressDO.getUserId());
        result.setReceiverName(addressDO.getReceiverName());
        result.setReceiverMobile(addressDO.getReceiverMobile());
        result.setProvince(addressDO.getProvince());
        result.setCity(addressDO.getCity());
        result.setDistrict(addressDO.getDistrict());
        result.setDetailAddress(addressDO.getDetailAddress());
        result.setPostalCode(addressDO.getPostalCode());
        result.setDefaultFlag(addressDO.getDefaultFlag());
        return result;
    }
}
