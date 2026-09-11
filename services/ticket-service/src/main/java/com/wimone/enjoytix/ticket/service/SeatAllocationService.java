package com.wimone.enjoytix.ticket.service;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.ticket.dao.entity.SeatStockDO;
import com.wimone.enjoytix.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeatAllocationService {

    private final TicketRepository repository;

    public SeatAllocationService(TicketRepository repository) {
        this.repository = repository;
    }

    public List<Long> allocateSeats(Long showId, Long categoryId, Long areaId, int quantity) {
        if (quantity <= 0) {
            return List.of();
        }
        List<SeatStockDO> seats = repository.listSeats(showId, categoryId, areaId);

        List<SeatAllocationCandidate> candidates = collectCandidates(seats, quantity);
        if (candidates.isEmpty()) {
            if (areaId == null) {
                throw new ClientException("Unable to allocate seats automatically");
            }
            throw new ClientException("Unable to allocate seats in selected area");
        }
        return candidates.get(0).seatIds();
    }

    private List<SeatAllocationCandidate> collectCandidates(List<SeatStockDO> seats, int quantity) {
        Map<Long, List<SeatStockDO>> byArea = seats.stream()
                .collect(Collectors.groupingBy(this::areaKey, Collectors.toCollection(ArrayList::new)));
        List<SeatAllocationCandidate> candidates = new ArrayList<>();
        for (Map.Entry<Long, List<SeatStockDO>> areaEntry : byArea.entrySet()) {
            Map<Integer, List<SeatStockDO>> byRow = areaEntry.getValue().stream()
                    .collect(Collectors.groupingBy(SeatStockDO::getRowNo, Collectors.toCollection(ArrayList::new)));
            for (Map.Entry<Integer, List<SeatStockDO>> rowEntry : byRow.entrySet()) {
                List<SeatStockDO> rowSeats = rowEntry.getValue().stream()
                        .sorted(Comparator.comparing(SeatStockDO::getColumnNo))
                        .toList();
                candidates.addAll(findRowCandidates(areaEntry.getKey(), rowEntry.getKey(), rowSeats, quantity));
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(SeatAllocationCandidate::rowNo)
                        .thenComparingInt(SeatAllocationCandidate::columnNo)
                        .thenComparing(SeatAllocationCandidate::areaId))
                .toList();
    }

    private List<SeatAllocationCandidate> findRowCandidates(Long areaId, Integer rowNo, List<SeatStockDO> rowSeats, int quantity) {
        List<SeatAllocationCandidate> candidates = new ArrayList<>();
        for (int start = 0; start <= rowSeats.size() - quantity; start++) {
            List<SeatStockDO> window = rowSeats.subList(start, start + quantity);
            if (isContinuous(window)) {
                candidates.add(new SeatAllocationCandidate(
                        areaId,
                        rowNo == null ? Integer.MAX_VALUE : rowNo,
                        window.get(0).getColumnNo() == null ? Integer.MAX_VALUE : window.get(0).getColumnNo(),
                        window.stream().map(SeatStockDO::getSeatId).toList()
                ));
            }
        }
        return candidates;
    }

    private boolean isContinuous(List<SeatStockDO> seats) {
        if (seats.isEmpty()) {
            return false;
        }
        for (int i = 1; i < seats.size(); i++) {
            Integer previous = seats.get(i - 1).getColumnNo();
            Integer current = seats.get(i).getColumnNo();
            if (previous == null || current == null || current - previous != 1) {
                return false;
            }
        }
        return true;
    }

    private Long areaKey(SeatStockDO seat) {
        return seat.getAreaId();
    }

    private record SeatAllocationCandidate(Long areaId, int rowNo, int columnNo, List<Long> seatIds) {
    }
}
