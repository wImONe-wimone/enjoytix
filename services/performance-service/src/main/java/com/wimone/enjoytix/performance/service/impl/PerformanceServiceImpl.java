package com.wimone.enjoytix.performance.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.resp.ArtistRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;
import com.wimone.enjoytix.performance.dto.resp.VenueRespDTO;
import com.wimone.enjoytix.performance.repository.PerformanceRepository;
import com.wimone.enjoytix.performance.service.PerformanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceRepository repository;

    public PerformanceServiceImpl(PerformanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<PerformanceListRespDTO> pageQuery(PerformancePageQueryReqDTO requestParam) {
        List<PerformanceListRespDTO> filtered = repository.listPerformances()
                .stream()
                .filter(each -> matches(each, requestParam))
                .map(this::convertList)
                .sorted(Comparator.comparing(PerformanceListRespDTO::earliestShowTime))
                .toList();
        long fromIndex = Math.max(0, (requestParam.getCurrent() - 1) * requestParam.getSize());
        long toIndex = Math.min(filtered.size(), fromIndex + requestParam.getSize());
        List<PerformanceListRespDTO> records = fromIndex >= filtered.size()
                ? List.of()
                : filtered.subList((int) fromIndex, (int) toIndex);
        return new PageResponse<>(requestParam.getCurrent(), requestParam.getSize(), filtered.size(), records);
    }

    @Override
    public PerformanceDetailRespDTO detail(Long performanceId) {
        PerformanceDO performance = findPerformance(performanceId);
        List<ShowSessionRespDTO> sessions = repository.listShowsByPerformance(performanceId)
                .stream()
                .map(this::convertShow)
                .toList();
        return new PerformanceDetailRespDTO(
                performance.getId(),
                performance.getTitle(),
                performance.getPerformanceType(),
                performance.getCity(),
                performance.getPosterUrl(),
                performance.getDescription(),
                performance.getStatus(),
                convertArtist(findArtist(performance.getArtistId())),
                convertVenue(findVenue(performance.getVenueId())),
                sessions
        );
    }

    @Override
    public ShowSessionRespDTO show(Long showId) {
        return convertShow(findShow(showId));
    }

    @Override
    public List<TicketCategoryRespDTO> ticketCategories(Long showId) {
        findShow(showId);
        return repository.listCategoriesByShow(showId).stream().map(this::convertCategory).toList();
    }

    @Override
    public SeatMapRespDTO seatMap(Long showId) {
        ShowSessionDO show = findShow(showId);
        HallDO hall = repository.findHall(show.getHallId()).orElseThrow(() -> new ClientException("Hall does not exist"));
        SeatMapDO seatMap = repository.findSeatMap(hall.getSeatMapId()).orElseThrow(() -> new ClientException("Seat map does not exist"));
        List<SeatRespDTO> seats = repository.listSeatsBySeatMap(seatMap.getId()).stream().map(this::convertSeat).toList();
        return new SeatMapRespDTO(seatMap.getId(), seatMap.getName(), seatMap.getRowCount(), seatMap.getColumnCount(), seats);
    }

    private boolean matches(PerformanceDO performance, PerformancePageQueryReqDTO requestParam) {
        return contains(performance.getCity(), requestParam.getCity())
                && contains(performance.getPerformanceType(), requestParam.getPerformanceType())
                && matchesArtist(performance, requestParam.getArtistName())
                && matchesVenue(performance, requestParam.getVenueName())
                && matchesShowDate(performance.getId(), requestParam.getShowDate());
    }

    private boolean matchesArtist(PerformanceDO performance, String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return true;
        }
        return contains(findArtist(performance.getArtistId()).getName(), artistName);
    }

    private boolean matchesVenue(PerformanceDO performance, String venueName) {
        if (venueName == null || venueName.isBlank()) {
            return true;
        }
        return contains(findVenue(performance.getVenueId()).getName(), venueName);
    }

    private boolean matchesShowDate(Long performanceId, LocalDate showDate) {
        if (showDate == null) {
            return true;
        }
        return repository.listShowsByPerformance(performanceId)
                .stream()
                .map(ShowSessionDO::getShowTime)
                .map(LocalDateTime::toLocalDate)
                .anyMatch(showDate::equals);
    }

    private boolean contains(String source, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(query.toLowerCase());
    }

    private PerformanceListRespDTO convertList(PerformanceDO performance) {
        LocalDateTime earliestShowTime = repository.listShowsByPerformance(performance.getId())
                .stream()
                .map(ShowSessionDO::getShowTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return new PerformanceListRespDTO(
                performance.getId(),
                performance.getTitle(),
                performance.getPerformanceType(),
                performance.getCity(),
                convertArtist(findArtist(performance.getArtistId())),
                convertVenue(findVenue(performance.getVenueId())),
                earliestShowTime,
                performance.getPosterUrl(),
                performance.getStatus()
        );
    }

    private PerformanceDO findPerformance(Long performanceId) {
        return repository.findPerformance(performanceId).orElseThrow(() -> new ClientException("Performance does not exist"));
    }

    private ArtistDO findArtist(Long artistId) {
        return repository.findArtist(artistId).orElseThrow(() -> new ClientException("Artist does not exist"));
    }

    private VenueDO findVenue(Long venueId) {
        return repository.findVenue(venueId).orElseThrow(() -> new ClientException("Venue does not exist"));
    }

    private ShowSessionDO findShow(Long showId) {
        return repository.findShow(showId).orElseThrow(() -> new ClientException("Show does not exist"));
    }

    private ArtistRespDTO convertArtist(ArtistDO artist) {
        return new ArtistRespDTO(artist.getId(), artist.getName(), artist.getDescription());
    }

    private VenueRespDTO convertVenue(VenueDO venue) {
        return new VenueRespDTO(venue.getId(), venue.getName(), venue.getCity(), venue.getAddress());
    }

    private ShowSessionRespDTO convertShow(ShowSessionDO show) {
        return new ShowSessionRespDTO(
                show.getId(),
                show.getPerformanceId(),
                show.getHallId(),
                show.getShowTime(),
                show.getSaleStartTime(),
                show.getSaleEndTime(),
                show.getStatus()
        );
    }

    private TicketCategoryRespDTO convertCategory(TicketCategoryDO category) {
        return new TicketCategoryRespDTO(
                category.getId(),
                category.getShowId(),
                category.getCategoryName(),
                category.getPrice(),
                category.getTotalStock(),
                category.getRemainingStock(),
                category.getSeatSelectable(),
                category.getStatus()
        );
    }

    private SeatRespDTO convertSeat(SeatDO seat) {
        return new SeatRespDTO(seat.getId(), seat.getAreaName(), seat.getRowNo(), seat.getColumnNo(), seat.getSeatNo(), seat.getStatus());
    }
}
