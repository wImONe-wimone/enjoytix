package com.wimone.enjoytix.user.repository;

import com.wimone.enjoytix.user.dao.entity.AttendeeDO;
import com.wimone.enjoytix.user.dao.entity.UserDO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository {

    private final Map<Long, UserDO> users = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final Map<Long, AttendeeDO> attendees = new ConcurrentHashMap<>();

    public Optional<UserDO> findUserByUsername(String username) {
        Long userId = usernameIndex.get(username);
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }

    public Optional<UserDO> findUserById(Long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public boolean existsUsername(String username) {
        return usernameIndex.containsKey(username);
    }

    public void saveUser(UserDO userDO) {
        users.put(userDO.getId(), userDO);
        usernameIndex.put(userDO.getUsername(), userDO.getId());
    }

    public void saveAttendee(AttendeeDO attendeeDO) {
        attendees.put(attendeeDO.getId(), attendeeDO);
    }

    public Optional<AttendeeDO> findAttendee(Long attendeeId) {
        return Optional.ofNullable(attendees.get(attendeeId));
    }

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
}
