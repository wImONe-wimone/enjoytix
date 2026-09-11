package com.wimone.enjoytix.ticket.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.ticket.common.enums.SeatStockStatusEnum;
import com.wimone.enjoytix.ticket.dao.entity.SeatStockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketIssueDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketLockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketStockDO;
import com.wimone.enjoytix.ticket.dao.mapper.SeatStockMapper;
import com.wimone.enjoytix.ticket.dao.mapper.TicketIssueMapper;
import com.wimone.enjoytix.ticket.dao.mapper.TicketLockMapper;
import com.wimone.enjoytix.ticket.dao.mapper.TicketStockMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
@Profile("mysql")
public class MyBatisPlusTicketRepository implements TicketRepository {

    private final TicketStockMapper ticketStockMapper;
    private final SeatStockMapper seatStockMapper;
    private final TicketLockMapper ticketLockMapper;
    private final TicketIssueMapper ticketIssueMapper;
    private final ConcurrentMap<Long, ReentrantLock> localShowLocks = new ConcurrentHashMap<>();

    public MyBatisPlusTicketRepository(
            TicketStockMapper ticketStockMapper,
            SeatStockMapper seatStockMapper,
            TicketLockMapper ticketLockMapper,
            TicketIssueMapper ticketIssueMapper) {
        this.ticketStockMapper = ticketStockMapper;
        this.seatStockMapper = seatStockMapper;
        this.ticketLockMapper = ticketLockMapper;
        this.ticketIssueMapper = ticketIssueMapper;
    }

    @Override
    public ReentrantLock lockForShow(Long showId) {
        return localShowLocks.computeIfAbsent(showId, key -> new ReentrantLock());
    }

    @Override
    public List<TicketStockDO> listStocks(Long showId) {
        return ticketStockMapper.selectList(Wrappers.lambdaQuery(TicketStockDO.class)
                .eq(TicketStockDO::getShowId, showId)
                .orderByDesc(TicketStockDO::getPrice));
    }

    @Override
    public Optional<TicketStockDO> findStock(Long showId, Long categoryId) {
        return Optional.ofNullable(ticketStockMapper.selectOne(Wrappers.lambdaQuery(TicketStockDO.class)
                .eq(TicketStockDO::getShowId, showId)
                .eq(TicketStockDO::getCategoryId, categoryId)
                .last("LIMIT 1")));
    }

    @Override
    public void saveStock(TicketStockDO stockDO) {
        if (ticketStockMapper.selectById(stockDO.getId()) == null) {
            ticketStockMapper.insert(stockDO);
            return;
        }
        ticketStockMapper.updateById(stockDO);
    }

    @Override
    public List<SeatStockDO> listSeats(Long showId) {
        return seatStockMapper.selectList(Wrappers.lambdaQuery(SeatStockDO.class)
                .eq(SeatStockDO::getShowId, showId)
                .orderByAsc(SeatStockDO::getRowNo)
                .orderByAsc(SeatStockDO::getColumnNo));
    }

    @Override
    public List<SeatStockDO> listSeats(Long showId, Long categoryId, Long areaId) {
        return seatStockMapper.selectList(Wrappers.lambdaQuery(SeatStockDO.class)
                .eq(SeatStockDO::getShowId, showId)
                .eq(categoryId != null, SeatStockDO::getCategoryId, categoryId)
                .eq(SeatStockDO::getStatus, SeatStockStatusEnum.AVAILABLE.name())
                .eq(areaId != null, SeatStockDO::getAreaId, areaId)
                .orderByAsc(SeatStockDO::getAreaId)
                .orderByAsc(SeatStockDO::getRowNo)
                .orderByAsc(SeatStockDO::getColumnNo));
    }

    @Override
    public Optional<SeatStockDO> findSeat(Long showId, Long seatId) {
        return Optional.ofNullable(seatStockMapper.selectOne(Wrappers.lambdaQuery(SeatStockDO.class)
                .eq(SeatStockDO::getShowId, showId)
                .eq(SeatStockDO::getSeatId, seatId)
                .last("LIMIT 1")));
    }

    @Override
    public void saveSeat(SeatStockDO seatDO) {
        if (seatStockMapper.selectById(seatDO.getId()) == null) {
            seatStockMapper.insert(seatDO);
            return;
        }
        seatStockMapper.update(null, Wrappers.lambdaUpdate(SeatStockDO.class)
                .eq(SeatStockDO::getId, seatDO.getId())
                .set(SeatStockDO::getShowId, seatDO.getShowId())
                .set(SeatStockDO::getCategoryId, seatDO.getCategoryId())
                .set(SeatStockDO::getSeatId, seatDO.getSeatId())
                .set(SeatStockDO::getAreaId, seatDO.getAreaId())
                .set(SeatStockDO::getRowNo, seatDO.getRowNo())
                .set(SeatStockDO::getColumnNo, seatDO.getColumnNo())
                .set(SeatStockDO::getSeatNo, seatDO.getSeatNo())
                .set(SeatStockDO::getStatus, seatDO.getStatus())
                .set(SeatStockDO::getLockId, seatDO.getLockId())
                .set(SeatStockDO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public void saveLock(TicketLockDO lockDO) {
        if (ticketLockMapper.selectById(lockDO.getId()) == null) {
            ticketLockMapper.insert(lockDO);
            return;
        }
        ticketLockMapper.updateById(lockDO);
    }

    @Override
    public Optional<TicketLockDO> findLock(Long lockId) {
        return Optional.ofNullable(ticketLockMapper.selectById(lockId));
    }

    @Override
    public List<TicketLockDO> listLocks(Long showId) {
        return ticketLockMapper.selectList(Wrappers.lambdaQuery(TicketLockDO.class)
                .eq(TicketLockDO::getShowId, showId)
                .orderByAsc(TicketLockDO::getExpireTime));
    }

    @Override
    public void saveIssue(TicketIssueDO issueDO) {
        if (ticketIssueMapper.selectById(issueDO.getId()) == null) {
            ticketIssueMapper.insert(issueDO);
            return;
        }
        ticketIssueMapper.updateById(issueDO);
    }

    @Override
    public List<TicketIssueDO> listIssuesByLockId(Long lockId) {
        return ticketIssueMapper.selectList(Wrappers.lambdaQuery(TicketIssueDO.class)
                .eq(TicketIssueDO::getLockId, lockId)
                .orderByAsc(TicketIssueDO::getId));
    }
}
