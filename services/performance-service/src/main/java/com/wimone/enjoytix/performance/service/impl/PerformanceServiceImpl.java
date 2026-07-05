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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceRepository repository;

    public PerformanceServiceImpl(PerformanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<PerformanceListRespDTO> pageQuery(PerformancePageQueryReqDTO requestParam) {
        PerformancePageQueryReqDTO query = requestParam == null ? new PerformancePageQueryReqDTO() : requestParam;
        long current = Math.max(1, query.getCurrent());
        long size = Math.min(Math.max(1, query.getSize()), 200);

        List<PerformanceDO> performances = emptyIfNull(repository.listPerformances())
                .stream()
                .filter(Objects::nonNull)
                .toList();
        List<Long> performanceIds = performances.stream()
                .map(PerformanceDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> artistIds = performances.stream()
                .map(PerformanceDO::getArtistId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> venueIds = performances.stream()
                .map(PerformanceDO::getVenueId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ArtistDO> artists = emptyIfNull(repository.listArtistsByIds(artistIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(each -> each.getId() != null)
                .collect(Collectors.toMap(ArtistDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, VenueDO> venues = emptyIfNull(repository.listVenuesByIds(venueIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(each -> each.getId() != null)
                .collect(Collectors.toMap(VenueDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<ShowSessionDO>> showsByPerformance = emptyIfNull(repository.listShowsByPerformanceIds(performanceIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(each -> each.getPerformanceId() != null)
                .collect(Collectors.groupingBy(ShowSessionDO::getPerformanceId));

        List<PerformanceListRespDTO> filtered = performances
                .stream()
                .filter(Objects::nonNull)
                .filter(each -> matches(
                        each,
                        query,
                        artists.get(each.getArtistId()),
                        venues.get(each.getVenueId()),
                        showsByPerformance.getOrDefault(each.getId(), List.of())
                ))
                .map(each -> convertList(
                        each,
                        artists.get(each.getArtistId()),
                        venues.get(each.getVenueId()),
                        showsByPerformance.getOrDefault(each.getId(), List.of())
                ))
                .sorted(Comparator.comparing(PerformanceListRespDTO::earliestShowTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(PerformanceListRespDTO::performanceId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        long fromIndex = pageOffset(current, size);
        List<PerformanceListRespDTO> records = fromIndex >= filtered.size()
                ? List.of()
                : filtered.subList((int) fromIndex, (int) Math.min(filtered.size(), fromIndex + size));
        return new PageResponse<>(current, size, filtered.size(), records);
    }

    @Override
    public PerformanceDetailRespDTO detail(Long performanceId) {
        PerformanceDO performance = findPerformance(performanceId);
        List<ShowSessionRespDTO> sessions = emptyIfNull(repository.listShowsByPerformance(performanceId))
                .stream()
                .filter(Objects::nonNull)
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
        return emptyIfNull(repository.listCategoriesByShow(showId))
                .stream()
                .filter(Objects::nonNull)
                .map(this::convertCategory)
                .toList();
    }

    @Override
    public SeatMapRespDTO seatMap(Long showId) {
        ShowSessionDO show = findShow(showId);
        HallDO hall = findHall(show.getHallId());
        SeatMapDO seatMap = findSeatMap(hall.getSeatMapId());
        List<SeatRespDTO> seats = emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(Objects::nonNull)
                .map(this::convertSeat)
                .toList();
        return new SeatMapRespDTO(seatMap.getId(), seatMap.getName(), seatMap.getRowCount(), seatMap.getColumnCount(), seats);
    }

    private boolean matches(
            PerformanceDO performance,
            PerformancePageQueryReqDTO requestParam,
            ArtistDO artist,
            VenueDO venue,
            List<ShowSessionDO> shows) {
        return contains(performance.getCity(), requestParam.getCity())
                && contains(performance.getPerformanceType(), requestParam.getPerformanceType())
                && matchesArtist(artist, requestParam.getArtistName())
                && matchesVenue(venue, requestParam.getVenueName())
                && matchesShowDate(shows, requestParam.getShowDate());
    }

    private boolean matchesArtist(ArtistDO artist, String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return true;
        }
        return artist != null && contains(artist.getName(), artistName);
    }

    private boolean matchesVenue(VenueDO venue, String venueName) {
        if (venueName == null || venueName.isBlank()) {
            return true;
        }
        return venue != null && contains(venue.getName(), venueName);
    }

    private boolean matchesShowDate(List<ShowSessionDO> shows, LocalDate showDate) {
        if (showDate == null) {
            return true;
        }
        return emptyIfNull(shows).stream()
                .map(ShowSessionDO::getShowTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .anyMatch(showDate::equals);
    }

    private boolean contains(String source, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return source != null && source.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private PerformanceListRespDTO convertList(PerformanceDO performance, ArtistDO artist, VenueDO venue, List<ShowSessionDO> shows) {
        LocalDateTime earliestShowTime = emptyIfNull(shows).stream()
                .map(ShowSessionDO::getShowTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return new PerformanceListRespDTO(
                performance.getId(),
                performance.getTitle(),
                performance.getPerformanceType(),
                performance.getCity(),
                convertArtist(artist),
                convertVenue(venue),
                earliestShowTime,
                performance.getPosterUrl(),
                performance.getStatus()
        );
    }

    private PerformanceDO findPerformance(Long performanceId) {
        if (performanceId == null) {
            throw new ClientException("Performance id is required");
        }
        return repository.findPerformance(performanceId).orElseThrow(() -> new ClientException("Performance does not exist"));
    }

    private ArtistDO findArtist(Long artistId) {
        if (artistId == null) {
            throw new ClientException("Artist does not exist");
        }
        return repository.findArtist(artistId).orElseThrow(() -> new ClientException("Artist does not exist"));
    }

    private VenueDO findVenue(Long venueId) {
        if (venueId == null) {
            throw new ClientException("Venue does not exist");
        }
        return repository.findVenue(venueId).orElseThrow(() -> new ClientException("Venue does not exist"));
    }

    private ShowSessionDO findShow(Long showId) {
        if (showId == null) {
            throw new ClientException("Show id is required");
        }
        return repository.findShow(showId).orElseThrow(() -> new ClientException("Show does not exist"));
    }

    private HallDO findHall(Long hallId) {
        if (hallId == null) {
            throw new ClientException("Hall does not exist");
        }
        return repository.findHall(hallId).orElseThrow(() -> new ClientException("Hall does not exist"));
    }

    private SeatMapDO findSeatMap(Long seatMapId) {
        if (seatMapId == null) {
            throw new ClientException("Seat map does not exist");
        }
        return repository.findSeatMap(seatMapId).orElseThrow(() -> new ClientException("Seat map does not exist"));
    }

    private long pageOffset(long current, long size) {
        long pageIndex = current - 1;
        if (pageIndex > Long.MAX_VALUE / size) {
            return Long.MAX_VALUE;
        }
        return pageIndex * size;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private ArtistRespDTO convertArtist(ArtistDO artist) {
        if (artist == null) {
            return null;
        }
        return new ArtistRespDTO(artist.getId(), artist.getName(), artist.getDescription());
    }

    private VenueRespDTO convertVenue(VenueDO venue) {
        if (venue == null) {
            return null;
        }
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
