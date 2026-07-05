package com.wimone.enjoytix.user.repository;

import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<UserDO> findUserByUsername(String username);

    Optional<UserDO> findUserById(Long userId);

    boolean existsUsername(String username);

    void saveUser(UserDO userDO);

    void saveAttendee(AttendeeDO attendeeDO);

    Optional<AttendeeDO> findAttendee(Long attendeeId);

    List<AttendeeDO> listAttendees(Long userId);
}
