package com.wimone.enjoytix.ticket.service.impl;

import com.wimone.enjoytix.framework.cache.lock.DistributedLockTemplate;
import com.wimone.enjoytix.framework.cache.lock.LocalDistributedLockTemplate;
import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.ticket.common.TicketLockKeys;
import com.wimone.enjoytix.ticket.common.enums.SeatStockStatusEnum;
import com.wimone.enjoytix.ticket.common.enums.TicketLockStatusEnum;
import com.wimone.enjoytix.ticket.dao.entity.SeatStockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketIssueDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketLockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketStockDO;
import com.wimone.enjoytix.ticket.dto.req.TicketIssueReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketLockReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketReleaseReqDTO;
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketIssueRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;
import com.wimone.enjoytix.ticket.repository.TicketRepository;
import com.wimone.enjoytix.ticket.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final long LOCK_WAIT_SECONDS = 1L;
    private static final long LOCK_LEASE_SECONDS = 30L;

    private final TicketRepository repository;
    private final IdGeneratorManager idGeneratorManager;
    private final long lockTtlMinutes;
    private final DistributedLockTemplate lockTemplate;

    @Autowired
    public TicketServiceImpl(
            TicketRepository repository,
            IdGeneratorManager idGeneratorManager,
            @Value("${ticket.lock.ttl-minutes:15}") long lockTtlMinutes,
            DistributedLockTemplate lockTemplate) {
        this.repository = repository;
        this.idGeneratorManager = idGeneratorManager;
        this.lockTtlMinutes = lockTtlMinutes;
        this.lockTemplate = lockTemplate;
    }

    public TicketServiceImpl(
            TicketRepository repository,
            IdGeneratorManager idGeneratorManager,
            long lockTtlMinutes) {
        this(repository, idGeneratorManager, lockTtlMinutes, new LocalDistributedLockTemplate());
    }

    @Override
    public List<TicketAvailabilityRespDTO> availability(Long showId) {
        return lockTemplate.execute(TicketLockKeys.showStock(showId), LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, () -> {
            expireLocks(showId);
            return repository.listStocks(showId).stream().map(this::convertStock).toList();
        });
    }

    @Override
    public List<SeatAvailabilityRespDTO> seats(Long showId) {
        return lockTemplate.execute(TicketLockKeys.showStock(showId), LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, () -> {
            expireLocks(showId);
            return repository.listSeats(showId).stream().map(this::convertSeat).toList();
        });
    }

    @Override
    public TicketLockRespDTO lock(Long userId, TicketLockReqDTO requestParam) {
        // 加上锁
        return lockTemplate.execute(TicketLockKeys.showStock(requestParam.getShowId()), LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, () -> {
            // 按场次id获取锁
            expireLocks(requestParam.getShowId());
            // 查询票档库存
            TicketStockDO stock = findStock(requestParam.getShowId(), requestParam.getCategoryId());
            List<Long> seatIds = normalizeSeatIds(requestParam.getSeatIds());
            int quantity = seatIds.isEmpty() ? normalizeQuantity(requestParam.getQuantity()) : seatIds.size();
            if (stock.availableStock() < quantity) {
                throw new ClientException("Insufficient ticket stock");
            }
            if (!seatIds.isEmpty()) {
                validateAndLockSeats(requestParam.getShowId(), requestParam.getCategoryId(), seatIds);
            }
            // 扣减库存，即增加锁定库存的数量
            stock.setLockedStock(stock.getLockedStock() + quantity);
            repository.saveStock(stock);
            // 保存锁定的票
            TicketLockDO lockDO = new TicketLockDO();
            lockDO.setId(idGeneratorManager.nextId());
            lockDO.setUserId(userId);
            lockDO.setShowId(requestParam.getShowId());
            lockDO.setCategoryId(requestParam.getCategoryId());
            lockDO.setQuantity(quantity);
            lockDO.setSeatIds(seatIds);
            lockDO.setStatus(TicketLockStatusEnum.LOCKED.name());
            lockDO.setExpireTime(LocalDateTime.now().plusMinutes(lockTtlMinutes));
            lockDO.setCreateTime(LocalDateTime.now());
            lockDO.setUpdateTime(LocalDateTime.now());
            lockDO.setDelFlag(0);
            if (!seatIds.isEmpty()) {
                seatIds.forEach(seatId -> repository.findSeat(requestParam.getShowId(), seatId).ifPresent(seat -> {
                    seat.setLockId(lockDO.getId());
                    repository.saveSeat(seat);
                }));
            }
            repository.saveLock(lockDO);
            return convertLock(lockDO);
        });
    }

    @Override
    public Boolean release(Long userId, TicketReleaseReqDTO requestParam) {
        // 根据 lockId 查询锁记录。
        TicketLockDO lockDO = findLock(requestParam.getLockId());
        return lockTemplate.execute(TicketLockKeys.showStock(lockDO.getShowId()), LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, () -> {
            assertOwner(userId, lockDO);
            releaseInternal(lockDO, TicketLockStatusEnum.RELEASED);
            return Boolean.TRUE;
        });
    }

    @Override
    public TicketIssueRespDTO issue(Long userId, TicketIssueReqDTO requestParam) {
        TicketLockDO lockDO = findLock(requestParam.getLockId());
        return lockTemplate.execute(TicketLockKeys.showStock(lockDO.getShowId()), LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS, () -> {
            assertOwner(userId, lockDO);
            if (!TicketLockStatusEnum.LOCKED.name().equals(lockDO.getStatus())) {
                throw new ClientException("Ticket lock is not active");
            }
            if (lockDO.getExpireTime().isBefore(LocalDateTime.now())) {
                releaseInternal(lockDO, TicketLockStatusEnum.EXPIRED);
                throw new ClientException("Ticket lock expired");
            }
            // lockedStock -= quantity，soldStock += quantity
            TicketStockDO stock = findStock(lockDO.getShowId(), lockDO.getCategoryId());
            stock.setLockedStock(stock.getLockedStock() - lockDO.getQuantity());
            stock.setSoldStock(stock.getSoldStock() + lockDO.getQuantity());
            repository.saveStock(stock);
            // 锁票的状态改为出票
            lockDO.setStatus(TicketLockStatusEnum.ISSUED.name());
            lockDO.setUpdateTime(LocalDateTime.now());
            repository.saveLock(lockDO);
            // 执行出票
            List<String> ticketCodes = issueTickets(requestParam.getOrderId(), lockDO);
            return new TicketIssueRespDTO(lockDO.getId(), requestParam.getOrderId(), ticketCodes);
        });
    }

    private void validateAndLockSeats(Long showId, Long categoryId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            SeatStockDO seat = repository.findSeat(showId, seatId).orElseThrow(() -> new ClientException("Seat does not exist"));
            if (!categoryId.equals(seat.getCategoryId())) {
                throw new ClientException("Seat does not belong to selected ticket category");
            }
            if (!SeatStockStatusEnum.AVAILABLE.name().equals(seat.getStatus())) {
                throw new ClientException("Seat is not available");
            }
        }
        for (Long seatId : seatIds) {
            SeatStockDO seat = repository.findSeat(showId, seatId).orElseThrow();
            seat.setStatus(SeatStockStatusEnum.LOCKED.name());
            repository.saveSeat(seat);
        }
    }

    private List<String> issueTickets(Long orderId, TicketLockDO lockDO) {
        List<String> ticketCodes = new ArrayList<>();
        List<Long> lockedSeatIds = normalizeSeatIds(lockDO.getSeatIds());
        List<Long> seatIds = lockedSeatIds.isEmpty() ? anonymousSeatIds(lockDO.getQuantity()) : lockedSeatIds;
        for (Long seatId : seatIds) {
            // 更改座位状态
            if (seatId > 0) {
                SeatStockDO seat = repository.findSeat(lockDO.getShowId(), seatId).orElseThrow();
                seat.setStatus(SeatStockStatusEnum.SOLD.name());
                seat.setLockId(lockDO.getId());
                repository.saveSeat(seat);
            }
            // 电子票记录
            TicketIssueDO issueDO = new TicketIssueDO();
            issueDO.setId(idGeneratorManager.nextId());
            issueDO.setLockId(lockDO.getId());
            issueDO.setOrderId(orderId);
            issueDO.setUserId(lockDO.getUserId());
            issueDO.setShowId(lockDO.getShowId());
            issueDO.setCategoryId(lockDO.getCategoryId());
            issueDO.setSeatId(seatId > 0 ? seatId : null);
            issueDO.setTicketCode("ET" + issueDO.getId());
            issueDO.setCreateTime(LocalDateTime.now());
            issueDO.setUpdateTime(LocalDateTime.now());
            issueDO.setDelFlag(0);
            repository.saveIssue(issueDO);
            // 生成电子码
            ticketCodes.add(issueDO.getTicketCode());
        }
        return ticketCodes;
    }

    private List<Long> anonymousSeatIds(Integer quantity) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            result.add(-1L - i);
        }
        return result;
    }

    private void expireLocks(Long showId) {
        LocalDateTime now = LocalDateTime.now();
        repository.listLocks(showId)
                .stream()
                .filter(each -> TicketLockStatusEnum.LOCKED.name().equals(each.getStatus()))
                .filter(each -> each.getExpireTime().isBefore(now))
                .forEach(each -> releaseInternal(each, TicketLockStatusEnum.EXPIRED));
    }

    private void releaseInternal(TicketLockDO lockDO, TicketLockStatusEnum targetStatus) {
        // 仅当锁状态为 LOCKED 时释放。
        if (!TicketLockStatusEnum.LOCKED.name().equals(lockDO.getStatus())) {
            return;
        }
        List<Long> seatIds = normalizeSeatIds(lockDO.getSeatIds());
        TicketStockDO stock = findStock(lockDO.getShowId(), lockDO.getCategoryId());
        // 返还库存
        stock.setLockedStock(Math.max(0, stock.getLockedStock() - lockDO.getQuantity()));
        repository.saveStock(stock);
        // 每个座位，状态恢复为 AVAILABLE
        for (Long seatId : seatIds) {
            repository.findSeat(lockDO.getShowId(), seatId).ifPresent(seat -> {
                seat.setStatus(SeatStockStatusEnum.AVAILABLE.name());
                // 清空lock id
                seat.setLockId(null);
                repository.saveSeat(seat);
            });
        }
        lockDO.setSeatIds(seatIds);
        lockDO.setStatus(targetStatus.name());
        lockDO.setUpdateTime(LocalDateTime.now());
        repository.saveLock(lockDO);
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        if (quantity > 6) {
            throw new ClientException("Single lock quantity cannot exceed 6");
        }
        return quantity;
    }

    private TicketStockDO findStock(Long showId, Long categoryId) {
        return repository.findStock(showId, categoryId).orElseThrow(() -> new ClientException("Ticket category does not exist"));
    }

    private TicketLockDO findLock(Long lockId) {
        return repository.findLock(lockId).orElseThrow(() -> new ClientException("Ticket lock does not exist"));
    }

    private void assertOwner(Long userId, TicketLockDO lockDO) {
        if (!lockDO.getUserId().equals(userId)) {
            throw new ClientException("Ticket lock does not belong to current user");
        }
    }

    private TicketAvailabilityRespDTO convertStock(TicketStockDO stock) {
        return new TicketAvailabilityRespDTO(
                stock.getShowId(),
                stock.getCategoryId(),
                stock.getCategoryName(),
                stock.getPrice(),
                stock.getTotalStock(),
                stock.getLockedStock(),
                stock.getSoldStock(),
                stock.availableStock(),
                stock.getSeatSelectable()
        );
    }

    private SeatAvailabilityRespDTO convertSeat(SeatStockDO seat) {
        return new SeatAvailabilityRespDTO(
                seat.getShowId(),
                seat.getCategoryId(),
                seat.getSeatId(),
                seat.getAreaName(),
                seat.getRowNo(),
                seat.getColumnNo(),
                seat.getSeatNo(),
                seat.getStatus()
        );
    }

    private TicketLockRespDTO convertLock(TicketLockDO lockDO) {
        List<Long> seatIds = normalizeSeatIds(lockDO.getSeatIds());
        return new TicketLockRespDTO(
                lockDO.getId(),
                lockDO.getShowId(),
                lockDO.getCategoryId(),
                lockDO.getQuantity(),
                seatIds,
                lockDO.getExpireTime()
        );
    }

    private List<Long> normalizeSeatIds(List<Long> seatIds) {
        if (seatIds == null) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>(seatIds.size());
        for (Object seatId : seatIds) {
            if (seatId instanceof Number number) {
                result.add(number.longValue());
            } else if (seatId != null) {
                result.add(Long.valueOf(seatId.toString()));
            }
        }
        return result;
    }
}
