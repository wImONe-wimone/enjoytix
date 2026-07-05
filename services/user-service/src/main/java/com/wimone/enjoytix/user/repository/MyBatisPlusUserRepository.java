package com.wimone.enjoytix.user.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dao.mapper.AttendeeMapper;
import com.wimone.enjoytix.user.dao.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusUserRepository implements UserRepository {

    private final UserMapper userMapper;
    private final AttendeeMapper attendeeMapper;

    public MyBatisPlusUserRepository(UserMapper userMapper, AttendeeMapper attendeeMapper) {
        this.userMapper = userMapper;
        this.attendeeMapper = attendeeMapper;
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
}
