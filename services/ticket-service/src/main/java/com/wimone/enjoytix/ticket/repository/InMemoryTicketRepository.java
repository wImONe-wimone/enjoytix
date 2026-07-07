package com.wimone.enjoytix.ticket.repository;

import com.wimone.enjoytix.ticket.common.enums.SeatStockStatusEnum;
import com.wimone.enjoytix.ticket.dao.entity.SeatStockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketIssueDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketLockDO;
import com.wimone.enjoytix.ticket.dao.entity.TicketStockDO;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
@Profile("!mysql")
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, TicketStockDO> stocks = new ConcurrentHashMap<>();
    private final Map<String, SeatStockDO> seats = new ConcurrentHashMap<>();
    private final Map<Long, TicketLockDO> locks = new ConcurrentHashMap<>();
    private final Map<Long, TicketIssueDO> issues = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> showLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSeedData() {
        stock(2001L, 3001L, "VIP", new BigDecimal("1280.00"), 12, 1);
        stock(2001L, 3002L, "A Zone", new BigDecimal("880.00"), 18, 1);
        stock(2001L, 3003L, "B Zone", new BigDecimal("580.00"), 18, 1);
        seedConcertSeats();

        stock(2002L, 3004L, "Standard", new BigDecimal("280.00"), 24, 1);
        seedDramaSeats();
    }

    @Override
    public ReentrantLock lockForShow(Long showId) {
        return showLocks.computeIfAbsent(showId, key -> new ReentrantLock());
    }

    @Override
    public List<TicketStockDO> listStocks(Long showId) {
        return stocks.values()
                .stream()
                .filter(each -> showId.equals(each.getShowId()))
                .sorted(Comparator.comparing(TicketStockDO::getPrice).reversed())
                .toList();
    }

    @Override
    public Optional<TicketStockDO> findStock(Long showId, Long categoryId) {
        return Optional.ofNullable(stocks.get(stockKey(showId, categoryId)));
    }

    @Override
    public void saveStock(TicketStockDO stockDO) {
        stocks.put(stockKey(stockDO.getShowId(), stockDO.getCategoryId()), stockDO);
    }

    @Override
    public List<SeatStockDO> listSeats(Long showId) {
        return seats.values()
                .stream()
                .filter(each -> showId.equals(each.getShowId()))
                .sorted(Comparator.comparing(SeatStockDO::getRowNo).thenComparing(SeatStockDO::getColumnNo))
                .toList();
    }

    @Override
    public Optional<SeatStockDO> findSeat(Long showId, Long seatId) {
        return Optional.ofNullable(seats.get(seatKey(showId, seatId)));
    }

    @Override
    public void saveSeat(SeatStockDO seatDO) {
        seats.put(seatKey(seatDO.getShowId(), seatDO.getSeatId()), seatDO);
    }

    @Override
    public void saveLock(TicketLockDO lockDO) {
        locks.put(lockDO.getId(), lockDO);
    }

    @Override
    public Optional<TicketLockDO> findLock(Long lockId) {
        return Optional.ofNullable(locks.get(lockId));
    }

    @Override
    public List<TicketLockDO> listLocks(Long showId) {
        return locks.values().stream().filter(each -> showId.equals(each.getShowId())).toList();
    }

    @Override
    public void saveIssue(TicketIssueDO issueDO) {
        issues.put(issueDO.getId(), issueDO);
    }

    @Override
    public List<TicketIssueDO> listIssuesByLockId(Long lockId) {
        return issues.values()
                .stream()
                .filter(each -> lockId.equals(each.getLockId()))
                .toList();
    }

    private void stock(Long showId, Long categoryId, String name, BigDecimal price, Integer totalStock, Integer seatSelectable) {
        TicketStockDO entity = new TicketStockDO();
        entity.setId(seedStockId(showId, categoryId));
        entity.setShowId(showId);
        entity.setCategoryId(categoryId);
        entity.setCategoryName(name);
        entity.setPrice(price);
        entity.setTotalStock(totalStock);
        entity.setLockedStock(0);
        entity.setSoldStock(0);
        entity.setSeatSelectable(seatSelectable);
        entity.setDelFlag(0);
        stocks.put(stockKey(showId, categoryId), entity);
    }

    private void seedConcertSeats() {
        long seatMapId = 400L;
        long base = seatMapId * 1000;
        for (int row = 1; row <= 6; row++) {
            for (int column = 1; column <= 8; column++) {
                Long categoryId = concertCategory(row, column);
                seat(2001L, categoryId, base + row * 100L + column, row <= 2 ? "Front" : "Standard", row, column);
            }
        }
    }

    private Long concertCategory(int row, int column) {
        if (row == 1 || row == 2 && column <= 4) {
            return 3001L;
        }
        if (row == 2 || row == 3 || row == 4 && column <= 6) {
            return 3002L;
        }
        return 3003L;
    }

    private void seedDramaSeats() {
        long seatMapId = 401L;
        long base = seatMapId * 1000;
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 6; column++) {
                seat(2002L, 3004L, base + row * 100L + column, row <= 2 ? "Front" : "Standard", row, column);
            }
        }
    }

    private void seat(Long showId, Long categoryId, Long seatId, String areaName, Integer rowNo, Integer columnNo) {
        SeatStockDO entity = new SeatStockDO();
        entity.setId(seatId);
        entity.setShowId(showId);
        entity.setCategoryId(categoryId);
        entity.setSeatId(seatId);
        entity.setAreaName(areaName);
        entity.setRowNo(rowNo);
        entity.setColumnNo(columnNo);
        entity.setSeatNo((char) ('A' + rowNo - 1) + String.valueOf(columnNo));
        entity.setStatus(SeatStockStatusEnum.AVAILABLE.name());
        entity.setDelFlag(0);
        seats.put(seatKey(showId, seatId), entity);
    }

    private String stockKey(Long showId, Long categoryId) {
        return showId + ":" + categoryId;
    }

    private String seatKey(Long showId, Long seatId) {
        return showId + ":" + seatId;
    }

    private Long seedStockId(Long showId, Long categoryId) {
        return showId * 100000L + categoryId;
    }
}
