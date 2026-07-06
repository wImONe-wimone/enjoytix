package com.wimone.enjoytix.user.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserAddressDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dao.entity.UserSessionDO;
import com.wimone.enjoytix.user.dao.mapper.AttendeeMapper;
import com.wimone.enjoytix.user.dao.mapper.UserAddressMapper;
import com.wimone.enjoytix.user.dao.mapper.UserMapper;
import com.wimone.enjoytix.user.dao.mapper.UserSessionMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusUserRepository implements UserRepository {

    private final UserMapper userMapper;
    private final AttendeeMapper attendeeMapper;
    private final UserAddressMapper userAddressMapper;
    private final UserSessionMapper userSessionMapper;

    public MyBatisPlusUserRepository(
            UserMapper userMapper,
            AttendeeMapper attendeeMapper,
            UserAddressMapper userAddressMapper,
            UserSessionMapper userSessionMapper) {
        this.userMapper = userMapper;
        this.attendeeMapper = attendeeMapper;
        this.userAddressMapper = userAddressMapper;
        this.userSessionMapper = userSessionMapper;
    }

    @Override
    public Optional<UserDO> findUserByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<UserDO> findUserById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    @Override
    public boolean existsUsername(String username) {
        return userMapper.selectCount(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)) > 0;
    }

    @Override
    public void saveUser(UserDO userDO) {
        if (userMapper.selectById(userDO.getId()) == null) {
            userMapper.insert(userDO);
            return;
        }
        userMapper.updateById(userDO);
    }

    @Override
    public void saveSession(UserSessionDO sessionDO) {
        if (sessionDO.getId() != null && userSessionMapper.selectById(sessionDO.getId()) != null) {
            userSessionMapper.updateById(sessionDO);
            return;
        }
        Optional<UserSessionDO> existing = findSessionByToken(sessionDO.getAccessToken());
        if (existing.isPresent()) {
            sessionDO.setId(existing.get().getId());
            userSessionMapper.updateById(sessionDO);
            return;
        }
        if (userSessionMapper.selectById(sessionDO.getId()) == null) {
            userSessionMapper.insert(sessionDO);
            return;
        }
        userSessionMapper.updateById(sessionDO);
    }

    @Override
    public Optional<UserSessionDO> findSessionByToken(String accessToken) {
        return Optional.ofNullable(userSessionMapper.selectOne(Wrappers.lambdaQuery(UserSessionDO.class)
                .eq(UserSessionDO::getAccessToken, accessToken)
                .last("LIMIT 1")));
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        UserSessionDO update = new UserSessionDO();
        update.setValidFlag(0);
        update.setLogoutTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        userSessionMapper.update(update, Wrappers.lambdaUpdate(UserSessionDO.class)
                .eq(UserSessionDO::getUserId, userId)
                .eq(UserSessionDO::getValidFlag, 1));
    }

    @Override
    public void saveAttendee(AttendeeDO attendeeDO) {
        if (Integer.valueOf(1).equals(attendeeDO.getDelFlag())) {
            attendeeMapper.deleteById(attendeeDO.getId());
            return;
        }
        if (attendeeMapper.selectById(attendeeDO.getId()) == null) {
            attendeeMapper.insert(attendeeDO);
            return;
        }
        attendeeMapper.updateById(attendeeDO);
    }

    @Override
    public Optional<AttendeeDO> findAttendee(Long attendeeId) {
        return Optional.ofNullable(attendeeMapper.selectById(attendeeId));
    }

    @Override
    public List<AttendeeDO> listAttendees(Long userId) {
        return attendeeMapper.selectList(Wrappers.lambdaQuery(AttendeeDO.class)
                .eq(AttendeeDO::getUserId, userId)
                .orderByDesc(AttendeeDO::getDefaultFlag)
                .orderByAsc(AttendeeDO::getId));
    }

    @Override
    public void clearDefaultAttendee(Long userId, Long exceptAttendeeId) {
        AttendeeDO update = new AttendeeDO();
        update.setDefaultFlag(0);
        update.setUpdateTime(LocalDateTime.now());
        attendeeMapper.update(update, Wrappers.lambdaUpdate(AttendeeDO.class)
                .eq(AttendeeDO::getUserId, userId)
                .eq(AttendeeDO::getDefaultFlag, 1)
                .ne(AttendeeDO::getId, exceptAttendeeId));
    }

    @Override
    public void saveAddress(UserAddressDO addressDO) {
        if (Integer.valueOf(1).equals(addressDO.getDelFlag())) {
            userAddressMapper.deleteById(addressDO.getId());
            return;
        }
        if (userAddressMapper.selectById(addressDO.getId()) == null) {
            userAddressMapper.insert(addressDO);
            return;
        }
        userAddressMapper.updateById(addressDO);
    }

    @Override
    public Optional<UserAddressDO> findAddress(Long addressId) {
        return Optional.ofNullable(userAddressMapper.selectById(addressId));
    }

    @Override
    public List<UserAddressDO> listAddresses(Long userId) {
        return userAddressMapper.selectList(Wrappers.lambdaQuery(UserAddressDO.class)
                .eq(UserAddressDO::getUserId, userId)
                .orderByDesc(UserAddressDO::getDefaultFlag)
                .orderByAsc(UserAddressDO::getId));
    }

    @Override
    public void clearDefaultAddress(Long userId, Long exceptAddressId) {
        UserAddressDO update = new UserAddressDO();
        update.setDefaultFlag(0);
        update.setUpdateTime(LocalDateTime.now());
        userAddressMapper.update(update, Wrappers.lambdaUpdate(UserAddressDO.class)
                .eq(UserAddressDO::getUserId, userId)
                .eq(UserAddressDO::getDefaultFlag, 1)
                .ne(UserAddressDO::getId, exceptAddressId));
    }
}
