package com.wimone.enjoytix.ticket.repository;

import com.wimone.enjoytix.ticket.dao.entity.SeatStockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketIssueDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketLockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketStockDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public interface TicketRepository {

    ReentrantLock lockForShow(Long showId);

    List<TicketStockDO> listStocks(Long showId);

    Optional<TicketStockDO> findStock(Long showId, Long categoryId);

    void saveStock(TicketStockDO stockDO);

    List<SeatStockDO> listSeats(Long showId);

    Optional<SeatStockDO> findSeat(Long showId, Long seatId);

    void saveSeat(SeatStockDO seatDO);

    void saveLock(TicketLockDO lockDO);

    Optional<TicketLockDO> findLock(Long lockId);

    List<TicketLockDO> listLocks(Long showId);

    void saveIssue(TicketIssueDO issueDO);

    List<TicketIssueDO> listIssuesByLockId(Long lockId);
}
