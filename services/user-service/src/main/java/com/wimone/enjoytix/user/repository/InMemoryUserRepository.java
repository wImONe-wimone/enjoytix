package com.wimone.enjoytix.user.repository;

import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserAddressDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import com.wimone.enjoytix.user.dao.entity.UserSessionDO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, UserDO> users = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final Map<Long, AttendeeDO> attendees = new ConcurrentHashMap<>();
    private final Map<Long, UserAddressDO> addresses = new ConcurrentHashMap<>();
    private final Map<String, UserSessionDO> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<UserDO> findUserByUsername(String username) {
        Long userId = usernameIndex.get(username);
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }

    @Override
    public Optional<UserDO> findUserById(Long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public boolean existsUsername(String username) {
        return usernameIndex.containsKey(username);
    }

    @Override
    public void saveUser(UserDO userDO) {
        users.put(userDO.getId(), userDO);
        usernameIndex.put(userDO.getUsername(), userDO.getId());
    }

    @Override
    public void saveSession(UserSessionDO sessionDO) {
        sessions.put(sessionDO.getAccessToken(), sessionDO);
    }

    @Override
    public Optional<UserSessionDO> findSessionByToken(String accessToken) {
        UserSessionDO sessionDO = sessions.get(accessToken);
        if (sessionDO == null || Integer.valueOf(1).equals(sessionDO.getDelFlag())) {
            return Optional.empty();
        }
        return Optional.of(sessionDO);
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        sessions.values().forEach(each -> {
            if (userId.equals(each.getUserId()) && Integer.valueOf(1).equals(each.getValidFlag())) {
                each.setValidFlag(0);
                each.setLogoutTime(now);
                each.setUpdateTime(now);
            }
        });
    }

    @Override
    public void saveAttendee(AttendeeDO attendeeDO) {
        attendees.put(attendeeDO.getId(), attendeeDO);
    }

    @Override
    public Optional<AttendeeDO> findAttendee(Long attendeeId) {
        AttendeeDO attendeeDO = attendees.get(attendeeId);
        if (attendeeDO == null || Integer.valueOf(1).equals(attendeeDO.getDelFlag())) {
            return Optional.empty();
        }
        return Optional.of(attendeeDO);
    }

    @Override
    public List<AttendeeDO> listAttendees(Long userId) {
        List<AttendeeDO> result = new ArrayList<>();
        for (AttendeeDO attendeeDO : attendees.values()) {
            if (userId.equals(attendeeDO.getUserId()) && Integer.valueOf(0).equals(attendeeDO.getDelFlag())) {
                result.add(attendeeDO);
            }
        }
        result.sort(Comparator.comparing(AttendeeDO::getDefaultFlag).reversed().thenComparing(AttendeeDO::getId));
        return result;
    }

    @Override
    public void clearDefaultAttendee(Long userId, Long exceptAttendeeId) {
        attendees.values().forEach(each -> {
            if (userId.equals(each.getUserId())
                    && !each.getId().equals(exceptAttendeeId)
                    && Integer.valueOf(0).equals(each.getDelFlag())
                    && Integer.valueOf(1).equals(each.getDefaultFlag())) {
                each.setDefaultFlag(0);
                each.setUpdateTime(LocalDateTime.now());
            }
        });
    }

    @Override
    public void saveAddress(UserAddressDO addressDO) {
        addresses.put(addressDO.getId(), addressDO);
    }

    @Override
    public Optional<UserAddressDO> findAddress(Long addressId) {
        UserAddressDO addressDO = addresses.get(addressId);
        if (addressDO == null || Integer.valueOf(1).equals(addressDO.getDelFlag())) {
            return Optional.empty();
        }
        return Optional.of(addressDO);
    }

    @Override
    public List<UserAddressDO> listAddresses(Long userId) {
        List<UserAddressDO> result = new ArrayList<>();
        for (UserAddressDO addressDO : addresses.values()) {
            if (userId.equals(addressDO.getUserId()) && Integer.valueOf(0).equals(addressDO.getDelFlag())) {
                result.add(addressDO);
            }
        }
        result.sort(Comparator.comparing(UserAddressDO::getDefaultFlag).reversed().thenComparing(UserAddressDO::getId));
        return result;
    }

    @Override
    public void clearDefaultAddress(Long userId, Long exceptAddressId) {
        addresses.values().forEach(each -> {
            if (userId.equals(each.getUserId())
                    && !each.getId().equals(exceptAddressId)
                    && Integer.valueOf(0).equals(each.getDelFlag())
                    && Integer.valueOf(1).equals(each.getDefaultFlag())) {
                each.setDefaultFlag(0);
                each.setUpdateTime(LocalDateTime.now());
            }
        });
    }
}
