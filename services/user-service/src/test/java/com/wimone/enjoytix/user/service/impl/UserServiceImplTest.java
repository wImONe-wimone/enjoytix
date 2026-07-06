package com.wimone.enjoytix.user.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.user.dto.req.AttendeeCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserAddressCreateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserOrderCancelReqDTO;
import com.wimone.enjoytix.user.dto.req.UserProfileUpdateReqDTO;
import com.wimone.enjoytix.user.dto.req.UserRegisterReqDTO;
import com.wimone.enjoytix.user.dto.resp.AttendeeRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserAddressRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserLoginRespDTO;
import com.wimone.enjoytix.user.dto.resp.UserRespDTO;
import com.wimone.enjoytix.user.remote.OrderRemoteService;
import com.wimone.enjoytix.user.remote.dto.OrderDetailRespDTO;
import com.wimone.enjoytix.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private InMemoryUserRepository userRepository;
    private UserServiceImpl userService;
    private AttendeeServiceImpl attendeeService;
    private UserAddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        AtomicLong sequence = new AtomicLong(1000);
        IdGeneratorManager idGeneratorManager = new IdGeneratorManager(sequence::getAndIncrement);
        userRepository = new InMemoryUserRepository();
        userService = new UserServiceImpl(userRepository, idGeneratorManager);
        attendeeService = new AttendeeServiceImpl(userRepository, idGeneratorManager);
        addressService = new UserAddressServiceImpl(userRepository, idGeneratorManager);
    }

    @Test
    void registerLoginUpdateAndLogout() {
        UserRespDTO user = userService.register(registerRequest("alice"));
        assertEquals(1000L, user.getUserId());

        UserLoginRespDTO login = userService.login(loginRequest("alice", "Passw0rd!"));
        assertEquals("dev-1000", login.getAccessToken());
        assertTrue(userRepository.findSessionByToken("dev-1000").orElseThrow().getValidFlag() == 1);

        UserProfileUpdateReqDTO updateReq = new UserProfileUpdateReqDTO();
        updateReq.setMobile("13900000000");
        updateReq.setRealName("Alice Updated");
        UserRespDTO updated = userService.updateProfile(user.getUserId(), updateReq);
        assertEquals("13900000000", updated.getMobile());
        assertEquals("Alice Updated", updated.getRealName());

        assertTrue(userService.logout(user.getUserId(), "Bearer dev-1000"));
        assertEquals(0, userRepository.findSessionByToken("dev-1000").orElseThrow().getValidFlag());
    }

    @Test
    void rejectDuplicateUsernameAndInvalidPassword() {
        userService.register(registerRequest("alice"));
        assertThrows(ClientException.class, () -> userService.register(registerRequest("alice")));
        assertThrows(ClientException.class, () -> userService.login(loginRequest("alice", "bad-password")));
    }

    @Test
    void keepOnlyOneDefaultAttendee() {
        Long userId = userService.register(registerRequest("alice")).getUserId();
        AttendeeRespDTO first = attendeeService.create(userId, attendeeRequest("Alice", "110101199001011234", 1));
        AttendeeRespDTO second = attendeeService.create(userId, attendeeRequest("Bob", "110101199001011235", 1));

        List<AttendeeRespDTO> attendees = attendeeService.list(userId);
        assertEquals(second.getAttendeeId(), attendees.get(0).getAttendeeId());
        assertEquals(1, attendees.get(0).getDefaultFlag());
        assertEquals(first.getAttendeeId(), attendees.get(1).getAttendeeId());
        assertEquals(0, attendees.get(1).getDefaultFlag());
    }

    @Test
    void keepOnlyOneDefaultAddress() {
        Long userId = userService.register(registerRequest("alice")).getUserId();
        UserAddressRespDTO first = addressService.create(userId, addressRequest("Alice", "Road 1", 1));
        UserAddressRespDTO second = addressService.create(userId, addressRequest("Bob", "Road 2", 1));

        List<UserAddressRespDTO> addresses = addressService.list(userId);
        assertEquals(second.getAddressId(), addresses.get(0).getAddressId());
        assertEquals(1, addresses.get(0).getDefaultFlag());
        assertEquals(first.getAddressId(), addresses.get(1).getAddressId());
        assertEquals(0, addresses.get(1).getDefaultFlag());
    }

    @Test
    void userOrderServiceDelegatesToOrderService() {
        OrderRemoteService remoteService = mock(OrderRemoteService.class);
        UserOrderServiceImpl userOrderService = new UserOrderServiceImpl(remoteService);
        OrderDetailRespDTO order = orderDetail(6001L);
        when(remoteService.list(1000L)).thenReturn(Result.success(List.of(order)));
        when(remoteService.detail(1000L, 6001L)).thenReturn(Result.success(order));
        when(remoteService.cancel(eq(1000L), any())).thenReturn(Result.success(Boolean.TRUE));

        assertEquals(1, userOrderService.list(1000L).size());
        assertEquals(6001L, userOrderService.detail(1000L, 6001L).orderId());
        UserOrderCancelReqDTO cancelReq = new UserOrderCancelReqDTO();
        cancelReq.setOrderId(6001L);
        assertTrue(userOrderService.cancel(1000L, cancelReq));
    }

    @Test
    void userOrderServicePropagatesRemoteFailure() {
        OrderRemoteService remoteService = mock(OrderRemoteService.class);
        when(remoteService.list(1000L)).thenReturn(Result.failure(
                com.wimone.enjoytix.framework.convention.errorcode.BaseErrorCode.REMOTE_ERROR,
                "order service unavailable"));
        UserOrderServiceImpl userOrderService = new UserOrderServiceImpl(remoteService);

        assertThrows(RemoteException.class, () -> userOrderService.list(1000L));
    }

    private UserRegisterReqDTO registerRequest(String username) {
        UserRegisterReqDTO request = new UserRegisterReqDTO();
        request.setUsername(username);
        request.setPassword("Passw0rd!");
        request.setMobile("13800000000");
        request.setRealName("Alice");
        return request;
    }

    private com.wimone.enjoytix.user.dto.req.UserLoginReqDTO loginRequest(String username, String password) {
        com.wimone.enjoytix.user.dto.req.UserLoginReqDTO request = new com.wimone.enjoytix.user.dto.req.UserLoginReqDTO();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private AttendeeCreateReqDTO attendeeRequest(String name, String certificateNo, Integer defaultFlag) {
        AttendeeCreateReqDTO request = new AttendeeCreateReqDTO();
        request.setRealName(name);
        request.setCertificateType("ID_CARD");
        request.setCertificateNo(certificateNo);
        request.setMobile("13800000000");
        request.setDefaultFlag(defaultFlag);
        return request;
    }

    private UserAddressCreateReqDTO addressRequest(String receiverName, String detailAddress, Integer defaultFlag) {
        UserAddressCreateReqDTO request = new UserAddressCreateReqDTO();
        request.setReceiverName(receiverName);
        request.setReceiverMobile("13800000000");
        request.setProvince("Shanghai");
        request.setCity("Shanghai");
        request.setDistrict("Pudong");
        request.setDetailAddress(detailAddress);
        request.setPostalCode("200120");
        request.setDefaultFlag(defaultFlag);
        return request;
    }

    private OrderDetailRespDTO orderDetail(Long orderId) {
        return new OrderDetailRespDTO(
                orderId,
                "EO" + orderId,
                1000L,
                2001L,
                5001L,
                new BigDecimal("1280.00"),
                "PENDING_PAYMENT",
                LocalDateTime.now().plusMinutes(15),
                List.of());
    }
}
