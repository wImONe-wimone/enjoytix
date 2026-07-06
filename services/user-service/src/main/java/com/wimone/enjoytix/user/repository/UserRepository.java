package com.wimone.enjoytix.user.repository;

import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserAddressDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dao.entity.UserSessionDO;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<UserDO> findUserByUsername(String username);

    Optional<UserDO> findUserById(Long userId);

    boolean existsUsername(String username);

    void saveUser(UserDO userDO);

    void saveSession(UserSessionDO sessionDO);

    Optional<UserSessionDO> findSessionByToken(String accessToken);

    void invalidateUserSessions(Long userId);

    void saveAttendee(AttendeeDO attendeeDO);

    Optional<AttendeeDO> findAttendee(Long attendeeId);

    List<AttendeeDO> listAttendees(Long userId);

    void clearDefaultAttendee(Long userId, Long exceptAttendeeId);

    void saveAddress(UserAddressDO addressDO);

    Optional<UserAddressDO> findAddress(Long addressId);

    List<UserAddressDO> listAddresses(Long userId);

    void clearDefaultAddress(Long userId, Long exceptAddressId);
}
