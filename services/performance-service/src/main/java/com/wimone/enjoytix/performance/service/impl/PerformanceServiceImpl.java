package com.wimone.enjoytix.performance.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.database.base.BaseDO;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.performance.common.enums.PerformanceSaleStatusEnum;
import com.wimone.enjoytix.performance.common.enums.PerformanceTypeEnum;
import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSeatCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;
import com.wimone.enjoytix.performance.dto.req.ArtistCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ArtistUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceSaleScheduleReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.SeatCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.SeatUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ShowSessionCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ShowSessionUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.TicketCategoryConfigReqDTO;
import com.wimone.enjoytix.performance.dto.req.VenueCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.VenueUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.resp.ArtistRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryConfigRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;
import com.wimone.enjoytix.performance.dto.resp.VenueRespDTO;
import com.wimone.enjoytix.performance.remote.TicketRemoteService;
import com.wimone.enjoytix.performance.remote.dto.TicketCategoryMappingReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketCategoryStockConfigReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketSeatStockConfigReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockConfigInitReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockInitReqDTO;
import com.wimone.enjoytix.performance.repository.PerformanceRepository;
import com.wimone.enjoytix.performance.service.PerformanceService;
import com.wimone.enjoytix.performance.message.PerformanceSaleStartMessage;
import com.wimone.enjoytix.performance.message.PerformanceSaleStartMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PerformanceServiceImpl implements PerformanceService {

    private static final int DEFAULT_SEAT_MAP_ROWS = 10;
    private static final int DEFAULT_SEAT_MAP_COLUMNS = 10;
    private static final int MAX_SEAT_MAP_ROWS = 200;
    private static final int MAX_SEAT_MAP_COLUMNS = 200;
    private static final int MAX_SEAT_MAP_CELLS = 10_000;
    private static final int TICKET_CATEGORY_STATUS_ENABLED = 1;

    private final PerformanceRepository repository;
    private final IdGeneratorManager idGeneratorManager;
    private final TicketRemoteService ticketRemoteService;
    private final PerformanceSaleStartMessageSender performanceSaleStartMessageSender;

    private record ValidatedTicketCategoryConfig(
            String categoryName,
            java.math.BigDecimal price,
            Integer totalStock,
            Integer seatSelectable,
            List<Long> seatIds,
            List<Long> lockedSeatIds) {
    }

    @Autowired
    public PerformanceServiceImpl(
            PerformanceRepository repository,
            IdGeneratorManager idGeneratorManager,
            TicketRemoteService ticketRemoteService,
            PerformanceSaleStartMessageSender performanceSaleStartMessageSender) {
        this.repository = repository;
        this.idGeneratorManager = idGeneratorManager;
        this.ticketRemoteService = ticketRemoteService;
        this.performanceSaleStartMessageSender = performanceSaleStartMessageSender;
    }

    public PerformanceServiceImpl(PerformanceRepository repository, IdGeneratorManager idGeneratorManager, TicketRemoteService ticketRemoteService) {
        this(repository, idGeneratorManager, ticketRemoteService, null);
    }

    public PerformanceServiceImpl(PerformanceRepository repository, IdGeneratorManager idGeneratorManager) {
        this(repository, idGeneratorManager, null, null);
    }

    @Override
    public PageResponse<PerformanceListRespDTO> pageQuery(PerformancePageQueryReqDTO requestParam) {
        PerformancePageQueryReqDTO query = requestParam == null ? new PerformancePageQueryReqDTO() : requestParam;
        long current = Math.max(1, query.getCurrent());
        long size = Math.min(Math.max(1, query.getSize()), 200);

        List<PerformanceDO> performances = emptyIfNull(repository.listPerformances())
                .stream()
                .filter(this::notDeleted)
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
                .filter(this::notDeleted)
                .filter(each -> each.getId() != null)
                .collect(Collectors.toMap(ArtistDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, VenueDO> venues = emptyIfNull(repository.listVenuesByIds(venueIds))
                .stream()
                .filter(this::notDeleted)
                .filter(each -> each.getId() != null)
                .collect(Collectors.toMap(VenueDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<ShowSessionDO>> showsByPerformance = emptyIfNull(repository.listShowsByPerformanceIds(performanceIds))
                .stream()
                .filter(this::notDeleted)
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
                .filter(this::notDeleted)
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
                saleStatus(performance).name(),
                performance.getScheduledSaleTime(),
                performance.getActualSaleTime(),
                convertArtist(findArtist(performance.getArtistId())),
                convertVenue(findVenue(performance.getVenueId())),
                sessions
        );
    }

    @Override
    @Transactional
    public PerformanceDetailRespDTO create(Long operatorId, PerformanceCreateReqDTO requestParam) {
        PerformanceDO performance = new PerformanceDO();
        performance.setId(idGeneratorManager.nextId());
        performance.setCreateTime(LocalDateTime.now());
        performance.setDelFlag(0);
        fillPerformance(performance, requestParam);
        performance.setSaleStatus(PerformanceSaleStatusEnum.PENDING_SALE.name());
        performance.setScheduledSaleTime(null);
        performance.setActualSaleTime(null);
        performance.setUpdateTime(LocalDateTime.now());
        repository.savePerformance(performance);
        for (ShowSessionCreateReqDTO showRequest : emptyIfNull(requestParam.getShowSessions())) {
            createShowInternal(operatorId, performance, showRequest);
        }
        return detail(performance.getId());
    }

    public PerformanceDetailRespDTO create(PerformanceCreateReqDTO requestParam) {
        return create(null, requestParam);
    }

    @Override
    @Transactional
    public PerformanceDetailRespDTO update(Long performanceId, PerformanceUpdateReqDTO requestParam) {
        PerformanceDO performance = findPerformance(performanceId);
        validatePerformanceVenueChange(performance, requestParam);
        fillPerformance(performance, requestParam);
        performance.setUpdateTime(LocalDateTime.now());
        repository.savePerformance(performance);
        return detail(performance.getId());
    }

    @Override
    @Transactional
    public Boolean delete(Long performanceId) {
        PerformanceDO performance = findPerformance(performanceId);
        if (hasActiveShows(performance.getId())) {
            throw new ClientException("Delete show sessions before deleting performance");
        }
        performance.setDelFlag(1);
        performance.setUpdateTime(LocalDateTime.now());
        repository.savePerformance(performance);
        return Boolean.TRUE;
    }

    @Override
    @Transactional
    public PerformanceDetailRespDTO startSaleNow(Long operatorId, Long performanceId) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        if (saleStatus(performance) == PerformanceSaleStatusEnum.ON_SALE) {
            throw new ClientException("Performance is already on sale");
        }
        return startSale(operatorId, performance, LocalDateTime.now(), true);
    }

    @Override
    @Transactional
    public PerformanceDetailRespDTO scheduleSale(Long operatorId, Long performanceId, PerformanceSaleScheduleReqDTO requestParam) {
        if (requestParam == null || requestParam.getSaleStartTime() == null) {
            throw new ClientException("Scheduled sale start time is required");
        }
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        assertPerformanceBeforeSale(performance, "Performance is already on sale");
        LocalDateTime scheduledSaleTime = PerformanceSaleStartMessage.normalizeSaleTime(requestParam.getSaleStartTime());
        if (!scheduledSaleTime.isAfter(LocalDateTime.now())) {
            throw new ClientException("Scheduled sale start time must be in the future");
        }
        validatePerformanceReadyForSale(performance, scheduledSaleTime);
        performance.setSaleStatus(PerformanceSaleStatusEnum.SCHEDULED.name());
        performance.setScheduledSaleTime(scheduledSaleTime);
        performance.setActualSaleTime(null);
        performance.setUpdateTime(LocalDateTime.now());
        repository.savePerformance(performance);
        sendSaleStartMessageAfterCommit(new PerformanceSaleStartMessage(operatorId, performance.getId(), scheduledSaleTime));
        return detail(performance.getId());
    }

    @Override
    @Transactional
    public PerformanceDetailRespDTO startScheduledSale(Long operatorId, Long performanceId, LocalDateTime scheduledSaleTime) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        PerformanceSaleStatusEnum currentStatus = saleStatus(performance);
        LocalDateTime normalizedSaleTime = PerformanceSaleStartMessage.normalizeSaleTime(scheduledSaleTime);
        if (currentStatus == PerformanceSaleStatusEnum.ON_SALE) {
            return detail(performance.getId());
        }
        if (currentStatus != PerformanceSaleStatusEnum.SCHEDULED) {
            return detail(performance.getId());
        }
        if (!Objects.equals(normalizedSaleTime, performance.getScheduledSaleTime())) {
            return detail(performance.getId());
        }
        if (performance.getScheduledSaleTime() != null && performance.getScheduledSaleTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Performance scheduled sale time has not arrived");
        }
        return startSale(operatorId, performance, LocalDateTime.now(), false);
    }

    @Override
    public List<ShowSessionRespDTO> adminShows(Long performanceId) {
        findPerformance(performanceId);
        return emptyIfNull(repository.listShowsByPerformance(performanceId))
                .stream()
                .filter(this::notDeleted)
                .map(this::convertShow)
                .toList();
    }

    @Override
    @Transactional
    public ShowSessionRespDTO createShow(Long operatorId, Long performanceId, ShowSessionCreateReqDTO requestParam) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        return createShowInternal(operatorId, performance, requestParam);
    }

    private ShowSessionRespDTO createShowInternal(Long operatorId, PerformanceDO performance, ShowSessionCreateReqDTO requestParam) {
        assertPerformanceBeforeSale(performance, "Show sessions can only be created before sale starts");
        HallDO hall = defaultHall(performance.getVenueId());
        ShowSessionDO sourceShow = sourceShow(performance.getId());
        ShowSessionDO show = new ShowSessionDO();
        show.setId(idGeneratorManager.nextId());
        show.setPerformanceId(performance.getId());
        show.setHallId(hall.getId());
        fillShow(show, requestParam);
        validateShowSchedule(show);
        show.setCreateTime(LocalDateTime.now());
        show.setUpdateTime(LocalDateTime.now());
        show.setDelFlag(0);
        repository.saveShow(show);
        if (hasTicketCategoryConfigs(requestParam.getTicketCategories())) {
            saveConfiguredTicketCategories(show, requestParam.getTicketCategories());
        } else {
            List<TicketCategoryMappingReqDTO> mappings = copyTicketCategories(sourceShow, show);
            copySeatCategoryMappings(sourceShow, show, mappings);
        }
        return convertShow(show);
    }

    @Override
    @Transactional
    public ShowSessionRespDTO updateShow(Long performanceId, Long showId, ShowSessionUpdateReqDTO requestParam) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        assertPerformanceBeforeSale(performance, "Show sessions can only be updated before sale starts");
        ShowSessionDO show = findPerformanceShow(performanceId, showId);
        fillShow(show, requestParam);
        validateShowSchedule(show);
        show.setUpdateTime(LocalDateTime.now());
        repository.saveShow(show);
        return convertShow(show);
    }

    @Override
    @Transactional
    public Boolean deleteShow(Long performanceId, Long showId) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        assertPerformanceBeforeSale(performance, "Show sessions can only be deleted before sale starts");
        ShowSessionDO show = findPerformanceShow(performanceId, showId);
        softDeleteShowTicketConfiguration(show.getId());
        show.setDelFlag(1);
        show.setUpdateTime(LocalDateTime.now());
        repository.saveShow(show);
        return Boolean.TRUE;
    }

    @Override
    public List<ArtistRespDTO> artists() {
        return emptyIfNull(repository.listArtists())
                .stream()
                .filter(this::notDeleted)
                .map(this::convertArtist)
                .toList();
    }

    @Override
    public ArtistRespDTO artist(Long artistId) {
        return convertArtist(findArtist(artistId));
    }

    @Override
    @Transactional
    public ArtistRespDTO createArtist(ArtistCreateReqDTO requestParam) {
        ArtistDO artist = new ArtistDO();
        artist.setId(idGeneratorManager.nextId());
        artist.setCreateTime(LocalDateTime.now());
        artist.setDelFlag(0);
        fillArtist(artist, requestParam);
        artist.setUpdateTime(LocalDateTime.now());
        repository.saveArtist(artist);
        return convertArtist(artist);
    }

    @Override
    @Transactional
    public ArtistRespDTO updateArtist(Long artistId, ArtistUpdateReqDTO requestParam) {
        ArtistDO artist = findArtist(artistId);
        fillArtist(artist, requestParam);
        artist.setUpdateTime(LocalDateTime.now());
        repository.saveArtist(artist);
        return convertArtist(artist);
    }

    @Override
    @Transactional
    public Boolean deleteArtist(Long artistId) {
        ArtistDO artist = findArtist(artistId);
        if (repository.countPerformancesByArtistId(artist.getId()) > 0) {
            throw new ClientException("Artist is referenced by performance projects");
        }
        artist.setDelFlag(1);
        artist.setUpdateTime(LocalDateTime.now());
        repository.saveArtist(artist);
        return Boolean.TRUE;
    }

    @Override
    public List<VenueRespDTO> venues() {
        return emptyIfNull(repository.listVenues())
                .stream()
                .filter(this::notDeleted)
                .map(this::convertVenue)
                .toList();
    }

    @Override
    public VenueRespDTO venue(Long venueId) {
        return convertVenue(findVenue(venueId));
    }

    @Override
    @Transactional
    public VenueRespDTO createVenue(VenueCreateReqDTO requestParam) {
        VenueDO venue = new VenueDO();
        venue.setId(idGeneratorManager.nextId());
        venue.setCreateTime(LocalDateTime.now());
        venue.setDelFlag(0);
        fillVenue(venue, requestParam);
        venue.setUpdateTime(LocalDateTime.now());
        repository.saveVenue(venue);
        syncVenueSeatLayout(venue, requestParam);
        return convertVenue(venue);
    }

    @Override
    @Transactional
    public VenueRespDTO updateVenue(Long venueId, VenueUpdateReqDTO requestParam) {
        VenueDO venue = findVenue(venueId);
        fillVenue(venue, requestParam);
        venue.setUpdateTime(LocalDateTime.now());
        repository.saveVenue(venue);
        syncVenueSeatLayout(venue, requestParam);
        return convertVenue(venue);
    }

    @Override
    @Transactional
    public Boolean deleteVenue(Long venueId) {
        VenueDO venue = findVenue(venueId);
        if (repository.countPerformancesByVenueId(venue.getId()) > 0) {
            throw new ClientException("Venue is referenced by performance projects");
        }
        venue.setDelFlag(1);
        venue.setUpdateTime(LocalDateTime.now());
        repository.saveVenue(venue);
        return Boolean.TRUE;
    }

    @Override
    public SeatMapRespDTO venueSeatMap(Long venueId) {
        SeatMapDO seatMap = defaultSeatMap(venueId);
        return convertSeatMap(seatMap);
    }

    @Override
    public List<SeatRespDTO> venueSeats(Long venueId) {
        return venueSeatMap(venueId).seats();
    }

    @Override
    @Transactional
    public SeatRespDTO createVenueSeat(Long venueId, SeatCreateReqDTO requestParam) {
        SeatMapDO seatMap = defaultSeatMap(venueId);
        SeatDO seat = new SeatDO();
        seat.setId(idGeneratorManager.nextId());
        seat.setSeatMapId(seatMap.getId());
        fillSeat(seat, requestParam, null);
        seat.setCreateTime(LocalDateTime.now());
        seat.setUpdateTime(LocalDateTime.now());
        seat.setDelFlag(0);
        expandSeatMapIfNeeded(seatMap, seat);
        repository.saveSeat(seat);
        return convertSeat(seat);
    }

    @Override
    @Transactional
    public SeatRespDTO updateVenueSeat(Long venueId, Long seatId, SeatUpdateReqDTO requestParam) {
        SeatMapDO seatMap = defaultSeatMap(venueId);
        SeatDO seat = findSeat(seatId);
        assertSeatBelongsToSeatMap(seat, seatMap);
        assertSeatNotConfigured(seat.getId(), "Configured seat cannot be updated");
        fillSeat(seat, requestParam, seat.getId());
        seat.setUpdateTime(LocalDateTime.now());
        expandSeatMapIfNeeded(seatMap, seat);
        repository.saveSeat(seat);
        return convertSeat(seat);
    }

    @Override
    @Transactional
    public Boolean deleteVenueSeat(Long venueId, Long seatId) {
        SeatMapDO seatMap = defaultSeatMap(venueId);
        SeatDO seat = findSeat(seatId);
        assertSeatBelongsToSeatMap(seat, seatMap);
        assertSeatNotConfigured(seat.getId(), "Configured seat cannot be deleted");
        seat.setDelFlag(1);
        seat.setUpdateTime(LocalDateTime.now());
        repository.saveSeat(seat);
        return Boolean.TRUE;
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
                .filter(this::notDeleted)
                .map(this::convertCategory)
                .toList();
    }

    @Override
    public List<TicketCategoryConfigRespDTO> ticketCategoryConfig(Long showId) {
        ShowSessionDO show = findShow(showId);
        return ticketCategoryConfigByShow(show);
    }

    @Override
    @Transactional
    public List<TicketCategoryConfigRespDTO> configureShowTicketCategories(Long operatorId, Long performanceId, Long showId, List<TicketCategoryConfigReqDTO> requestParam) {
        PerformanceDO performance = findPerformanceForUpdate(performanceId);
        assertPerformanceBeforeSale(performance, "Ticket categories can only be configured before sale starts");
        ShowSessionDO show = findPerformanceShow(performanceId, showId);
        softDeleteShowTicketConfiguration(show.getId());
        saveConfiguredTicketCategories(show, requestParam);
        return ticketCategoryConfigByShow(show);
    }

    @Override
    public SeatMapRespDTO seatMap(Long showId) {
        ShowSessionDO show = findShow(showId);
        HallDO hall = findHall(show.getHallId());
        SeatMapDO seatMap = findSeatMap(hall.getSeatMapId());
        List<SeatRespDTO> seats = emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(this::notDeleted)
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
        return contains(performance.getTitle(), requestParam.getTitle())
                && contains(performance.getPerformanceType(), requestParam.getPerformanceType())
                && matchesStatus(performance, requestParam.getStatus())
                && matchesArtist(artist, requestParam.getArtistName())
                && matchesVenue(venue, requestParam.getVenueName())
                && matchesShowDate(shows, requestParam.getShowDate());
    }

    private boolean matchesStatus(PerformanceDO performance, Integer status) {
        return status == null || Objects.equals(performance.getStatus(), status);
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

    private void fillPerformance(PerformanceDO performance, PerformanceCreateReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Performance request is required");
        }
        ArtistDO artist = findArtist(requestParam.getArtistId());
        VenueDO venue = findVenue(requestParam.getVenueId());
        String venueCity = trimToNull(venue.getCity());
        if (venueCity == null) {
            throw new ClientException("Venue city is required");
        }
        performance.setTitle(trimRequired(requestParam.getTitle(), "Performance title is required"));
        performance.setPerformanceType(normalizePerformanceType(requestParam.getPerformanceType()));
        performance.setArtistId(artist.getId());
        performance.setVenueId(venue.getId());
        performance.setCity(venueCity);
        performance.setPosterUrl(trimToNull(requestParam.getPosterUrl()));
        performance.setDescription(trimToNull(requestParam.getDescription()));
        performance.setStatus(normalizeStatus(requestParam.getStatus()));
    }

    private void validatePerformanceVenueChange(PerformanceDO performance, PerformanceCreateReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Performance request is required");
        }
        Long targetVenueId = requestParam.getVenueId();
        if (targetVenueId == null) {
            throw new ClientException("Venue does not exist");
        }
        if (!Objects.equals(performance.getVenueId(), targetVenueId) && hasActiveShows(performance.getId())) {
            throw new ClientException("Performance venue cannot be changed after show sessions are created");
        }
    }

    private void fillArtist(ArtistDO artist, ArtistCreateReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Artist request is required");
        }
        artist.setName(trimRequired(requestParam.getName(), "Artist name is required"));
        artist.setDescription(trimToNull(requestParam.getDescription()));
    }

    private void fillVenue(VenueDO venue, VenueCreateReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Venue request is required");
        }
        venue.setName(trimRequired(requestParam.getName(), "Venue name is required"));
        venue.setCountry(defaultIfBlank(requestParam.getCountry(), "中国"));
        venue.setProvince(trimRequired(requestParam.getProvince(), "Venue province is required"));
        venue.setCity(trimRequired(requestParam.getCity(), "Venue city is required"));
        venue.setDistrict(trimRequired(requestParam.getDistrict(), "Venue district is required"));
        venue.setTown(trimToNull(requestParam.getTown()));
        venue.setVillage(trimToNull(requestParam.getVillage()));
        venue.setStreet(trimRequired(requestParam.getStreet(), "Venue street is required"));
        venue.setHouseNumber(trimRequired(requestParam.getHouseNumber(), "Venue house number is required"));
        venue.setEstate(trimToNull(requestParam.getEstate()));
        venue.setBuilding(trimToNull(requestParam.getBuilding()));
        String address = trimToNull(requestParam.getAddress());
        venue.setAddress(address == null ? trimRequired(composeVenueAddress(venue), "Venue address is required") : address);
    }

    private void fillShow(ShowSessionDO show, ShowSessionCreateReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("Show session request is required");
        }
        LocalDateTime showTime = requestParam.getShowTime();
        if (showTime == null) {
            throw new ClientException("Show time is required");
        }
        Integer durationMinutes = requestParam.getDurationMinutes();
        if (durationMinutes == null || durationMinutes < 1 || durationMinutes > 1440) {
            throw new ClientException("Show duration must be between 1 and 1440 minutes");
        }
        show.setShowTime(showTime);
        show.setDurationMinutes(durationMinutes);
        if (show.getSaleStartTime() == null) {
            show.setSaleStartTime(LocalDateTime.now());
        }
        show.setSaleEndTime(showTime.minusMinutes(30));
        show.setStatus(normalizeStatus(requestParam.getStatus()));
    }

    private void fillSeat(SeatDO seat, SeatCreateReqDTO requestParam, Long selfSeatId) {
        if (requestParam == null) {
            throw new ClientException("Seat request is required");
        }
        Long areaId = requestParam.getAreaId();
        if (areaId == null) {
            throw new ClientException("Seat area id is required");
        }
        String seatNo = trimRequired(requestParam.getSeatNo(), "Seat number is required");
        Integer rowNo = requestParam.getRowNo();
        Integer columnNo = requestParam.getColumnNo();
        if (rowNo == null || rowNo < 1 || rowNo > 1000 || columnNo == null || columnNo < 1 || columnNo > 1000) {
            throw new ClientException("Seat row and column must be between 1 and 1000");
        }
        repository.findSeatBySeatMapPosition(seat.getSeatMapId(), rowNo, columnNo)
                .filter(this::notDeleted)
                .filter(existing -> !Objects.equals(existing.getId(), selfSeatId))
                .ifPresent(existing -> {
                    throw new ClientException("Seat position already exists");
                });
        boolean duplicateSeatNo = emptyIfNull(repository.listSeatsBySeatMap(seat.getSeatMapId()))
                .stream()
                .filter(this::notDeleted)
                .filter(existing -> !Objects.equals(existing.getId(), selfSeatId))
                .anyMatch(existing -> seatNo.equalsIgnoreCase(existing.getSeatNo()));
        if (duplicateSeatNo) {
            throw new ClientException("Seat number already exists");
        }
        seat.setAreaId(areaId);
        seat.setRowNo(rowNo);
        seat.setColumnNo(columnNo);
        seat.setSeatNo(seatNo);
        seat.setStatus(normalizeStatus(requestParam.getStatus()));
    }

    private void validateShowSchedule(ShowSessionDO show) {
        HallDO hall = findHall(show.getHallId());
        findSeatMap(hall.getSeatMapId());
        LocalDateTime showStart = show.getShowTime();
        LocalDateTime showEnd = showStart.plusMinutes(show.getDurationMinutes());
        if (!show.getSaleEndTime().isAfter(show.getSaleStartTime())) {
            throw new ClientException("Show sale end time must be after sale start time");
        }
        for (ShowSessionDO existing : emptyIfNull(repository.listShowsByHall(hall.getId()))) {
            if (!notDeleted(existing) || Objects.equals(existing.getId(), show.getId())) {
                continue;
            }
            if (existing.getShowTime() == null || existing.getDurationMinutes() == null) {
                continue;
            }
            LocalDateTime existingStart = existing.getShowTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());
            if (showStart.isBefore(existingEnd) && showEnd.isAfter(existingStart)) {
                throw new ClientException("Show time conflicts with another show in the same hall");
            }
        }
    }

    private boolean hasTicketCategoryConfigs(List<TicketCategoryConfigReqDTO> configs) {
        return configs != null && !configs.isEmpty();
    }

    private void saveConfiguredTicketCategories(ShowSessionDO show, List<TicketCategoryConfigReqDTO> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new ClientException("Ticket category configs are required");
        }
        HallDO hall = findHall(show.getHallId());
        SeatMapDO seatMap = findSeatMap(hall.getSeatMapId());
        Map<Long, SeatDO> seats = emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(this::notDeleted)
                .filter(each -> Integer.valueOf(1).equals(each.getStatus()))
                .collect(Collectors.toMap(SeatDO::getId, Function.identity(), (left, right) -> left));
        Set<String> categoryNames = new HashSet<>();
        Set<Long> configuredSeatIds = new HashSet<>();
        List<ValidatedTicketCategoryConfig> validatedConfigs = new ArrayList<>();
        for (TicketCategoryConfigReqDTO config : configs) {
            validatedConfigs.add(validateConfiguredTicketCategory(show, hall, seatMap, seats, categoryNames, configuredSeatIds, config));
        }
        for (ValidatedTicketCategoryConfig config : validatedConfigs) {
            TicketCategoryDO category = createConfiguredCategory(show.getId(), config);
            Set<Long> lockedSeatIds = new HashSet<>(config.lockedSeatIds());
            for (Long seatId : config.seatIds()) {
                ShowSeatCategoryDO mapping = new ShowSeatCategoryDO();
                mapping.setId(idGeneratorManager.nextId());
                mapping.setShowId(show.getId());
                mapping.setCategoryId(category.getId());
                mapping.setSeatId(seatId);
                mapping.setSaleLocked(lockedSeatIds.contains(seatId) ? 1 : 0);
                mapping.setCreateTime(LocalDateTime.now());
                mapping.setUpdateTime(LocalDateTime.now());
                mapping.setDelFlag(0);
                repository.saveSeatCategoryMapping(mapping);
            }
        }
    }

    private ValidatedTicketCategoryConfig validateConfiguredTicketCategory(
            ShowSessionDO show,
            HallDO hall,
            SeatMapDO seatMap,
            Map<Long, SeatDO> seats,
            Set<String> categoryNames,
            Set<Long> configuredSeatIds,
            TicketCategoryConfigReqDTO config) {
        if (config == null) {
            throw new ClientException("Ticket category config is required");
        }
        if (config.getSeatMapId() != null && !Objects.equals(config.getSeatMapId(), seatMap.getId())) {
            throw new ClientException("Ticket category seat map does not match show venue: configuredSeatMapId="
                    + config.getSeatMapId() + ", showSeatMapId=" + seatMap.getId() + ", showId=" + show.getId());
        }
        String categoryName = trimRequired(config.getCategoryName(), "Ticket category name is required");
        String nameKey = categoryName.toLowerCase(Locale.ROOT);
        if (!categoryNames.add(nameKey)) {
            throw new ClientException("Duplicate ticket category name");
        }
        if (config.getPrice() == null || config.getPrice().signum() <= 0) {
            throw new ClientException("Ticket category price must be positive");
        }
        if (config.getTotalStock() == null || config.getTotalStock() <= 0) {
            throw new ClientException("Ticket category total stock must be positive");
        }
        Integer seatSelectable = normalizeStatus(config.getSeatSelectable());
        List<Long> seatIds = validateSeatIds(config.getSeatIds());
        List<Long> lockedSeatIds = validateSeatIds(config.getLockedSeatIds());
        if (Integer.valueOf(1).equals(seatSelectable)) {
            if (seatIds.isEmpty()) {
                throw new ClientException("Selectable ticket category seats are required");
            }
            if (!Objects.equals(config.getTotalStock(), seatIds.size())) {
                throw new ClientException("Selectable ticket category stock must equal assigned seat count");
            }
            Set<Long> categorySeatIds = new HashSet<>(seatIds);
            for (Long lockedSeatId : lockedSeatIds) {
                if (!categorySeatIds.contains(lockedSeatId)) {
                    throw new ClientException("Locked seat must belong to the same ticket category");
                }
            }
            for (Long seatId : seatIds) {
                SeatDO seat = seats.get(seatId);
                if (seat == null) {
                    throw new ClientException("Configured seat does not exist in show venue: seatId=" + seatId
                            + ", showId=" + show.getId() + ", hallId=" + hall.getId() + ", seatMapId=" + seatMap.getId());
                }
                if (!configuredSeatIds.add(seatId)) {
                    throw new ClientException("Seat can only belong to one ticket category in a show");
                }
            }
        } else {
            if (!lockedSeatIds.isEmpty()) {
                throw new ClientException("Locked seats require selectable ticket category seats");
            }
            seatIds = List.of();
            lockedSeatIds = List.of();
        }
        return new ValidatedTicketCategoryConfig(
                categoryName,
                config.getPrice(),
                config.getTotalStock(),
                seatSelectable,
                seatIds,
                lockedSeatIds
        );
    }

    private TicketCategoryDO createConfiguredCategory(Long showId, ValidatedTicketCategoryConfig config) {
        TicketCategoryDO category = new TicketCategoryDO();
        category.setId(idGeneratorManager.nextId());
        category.setShowId(showId);
        category.setCategoryName(config.categoryName());
        category.setPrice(config.price());
        category.setTotalStock(config.totalStock());
        category.setRemainingStock(config.totalStock());
        category.setSeatSelectable(config.seatSelectable());
        category.setStatus(TICKET_CATEGORY_STATUS_ENABLED);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDelFlag(0);
        repository.saveCategory(category);
        return category;
    }

    private List<Long> validateSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long seatId : seatIds) {
            if (seatId == null) {
                throw new ClientException("Seat id is required");
            }
            if (!seen.add(seatId)) {
                throw new ClientException("Seat can only belong to one ticket category in a show");
            }
            result.add(seatId);
        }
        return result;
    }

    private ShowSessionDO sourceShow(Long performanceId) {
        return emptyIfNull(repository.listShowsByPerformance(performanceId))
                .stream()
                .filter(this::notDeleted)
                .filter(each -> hasActiveCategories(each.getId()))
                .findFirst()
                .orElse(null);
    }

    private List<TicketCategoryMappingReqDTO> copyTicketCategories(ShowSessionDO sourceShow, ShowSessionDO targetShow) {
        if (sourceShow == null) {
            return List.of();
        }
        List<TicketCategoryMappingReqDTO> mappings = new ArrayList<>();
        for (TicketCategoryDO source : emptyIfNull(repository.listCategoriesByShow(sourceShow.getId()))) {
            if (!notDeleted(source)) {
                continue;
            }
            TicketCategoryDO target = new TicketCategoryDO();
            target.setId(idGeneratorManager.nextId());
            target.setShowId(targetShow.getId());
            target.setCategoryName(source.getCategoryName());
            target.setPrice(source.getPrice());
            target.setTotalStock(source.getTotalStock());
            target.setRemainingStock(source.getTotalStock());
            target.setSeatSelectable(source.getSeatSelectable());
            target.setStatus(TICKET_CATEGORY_STATUS_ENABLED);
            target.setCreateTime(LocalDateTime.now());
            target.setUpdateTime(LocalDateTime.now());
            target.setDelFlag(0);
            repository.saveCategory(target);
            mappings.add(new TicketCategoryMappingReqDTO(source.getId(), target.getId()));
        }
        return mappings;
    }

    private void copySeatCategoryMappings(ShowSessionDO sourceShow, ShowSessionDO targetShow, List<TicketCategoryMappingReqDTO> mappings) {
        if (sourceShow == null || mappings == null || mappings.isEmpty()) {
            return;
        }
        Map<Long, Long> sourceToTargetCategoryIds = mappings.stream()
                .collect(Collectors.toMap(TicketCategoryMappingReqDTO::sourceCategoryId, TicketCategoryMappingReqDTO::targetCategoryId, (left, right) -> left));
        for (ShowSeatCategoryDO source : emptyIfNull(repository.listSeatCategoryMappingsByShow(sourceShow.getId()))) {
            if (!notDeleted(source)) {
                continue;
            }
            Long targetCategoryId = sourceToTargetCategoryIds.get(source.getCategoryId());
            if (targetCategoryId == null) {
                continue;
            }
            ShowSeatCategoryDO target = new ShowSeatCategoryDO();
            target.setId(idGeneratorManager.nextId());
            target.setShowId(targetShow.getId());
            target.setCategoryId(targetCategoryId);
            target.setSeatId(source.getSeatId());
            target.setSaleLocked(Integer.valueOf(1).equals(source.getSaleLocked()) ? 1 : 0);
            target.setCreateTime(LocalDateTime.now());
            target.setUpdateTime(LocalDateTime.now());
            target.setDelFlag(0);
            repository.saveSeatCategoryMapping(target);
        }
    }

    private void softDeleteShowTicketConfiguration(Long showId) {
        repository.deleteSeatCategoryMappingsByShow(showId);
        for (TicketCategoryDO category : emptyIfNull(repository.listCategoriesByShow(showId))) {
            if (!notDeleted(category)) {
                continue;
            }
            category.setDelFlag(1);
            category.setUpdateTime(LocalDateTime.now());
            repository.saveCategory(category);
        }
    }

    private void initTicketStock(Long operatorId, ShowSessionDO sourceShow, ShowSessionDO targetShow, List<TicketCategoryMappingReqDTO> mappings) {
        if (ticketRemoteService == null || sourceShow == null || mappings == null || mappings.isEmpty()) {
            return;
        }
        Result<Boolean> result = ticketRemoteService.initShowStock(
                operatorId,
                new TicketShowStockInitReqDTO(sourceShow.getId(), targetShow.getId(), mappings)
        );
        if (result == null || !result.isSuccess()) {
            throw new RemoteException("Init ticket stock failed: " + (result == null ? "empty result" : result.getMessage()));
        }
    }

    private void initConfiguredTicketStock(Long operatorId, ShowSessionDO show) {
        if (ticketRemoteService == null) {
            return;
        }
        Result<Boolean> result = ticketRemoteService.initConfiguredShowStock(
                operatorId,
                new TicketShowStockConfigInitReqDTO(show.getId(), configuredStockCategories(show))
        );
        if (result == null || !result.isSuccess()) {
            throw new RemoteException("Init configured ticket stock failed: " + (result == null ? "empty result" : result.getMessage()));
        }
    }

    private List<TicketCategoryStockConfigReqDTO> configuredStockCategories(ShowSessionDO show) {
        Map<Long, List<ShowSeatCategoryDO>> mappingsByCategory = emptyIfNull(repository.listSeatCategoryMappingsByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .collect(Collectors.groupingBy(ShowSeatCategoryDO::getCategoryId));
        Map<Long, SeatDO> seats = showSeats(show)
                .stream()
                .filter(this::notDeleted)
                .collect(Collectors.toMap(SeatDO::getId, Function.identity(), (left, right) -> left));
        return emptyIfNull(repository.listCategoriesByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .map(category -> new TicketCategoryStockConfigReqDTO(
                        category.getId(),
                        category.getCategoryName(),
                        category.getPrice(),
                        category.getTotalStock(),
                        category.getSeatSelectable(),
                        emptyIfNull(mappingsByCategory.get(category.getId()))
                                .stream()
                                .map(mapping -> {
                                    SeatDO seat = seats.get(mapping.getSeatId());
                                    if (seat == null) {
                                        return null;
                                    }
                                    return new TicketSeatStockConfigReqDTO(
                                            seat.getId(),
                                            seat.getAreaId(),
                                            seat.getRowNo(),
                                            seat.getColumnNo(),
                                            seat.getSeatNo(),
                                            Integer.valueOf(1).equals(mapping.getSaleLocked()));
                                })
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList();
    }

    private String normalizePerformanceType(String value) {
        String normalized = trimRequired(value, "Performance type is required").toUpperCase(Locale.ROOT);
        try {
            PerformanceTypeEnum.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new ClientException("Unsupported performance type: " + value);
        }
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new ClientException("Performance status must be 0 or 1");
        }
        return status;
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ClientException(message);
        }
        return trimmed;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
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
                performance.getStatus(),
                saleStatus(performance).name(),
                performance.getScheduledSaleTime(),
                performance.getActualSaleTime()
        );
    }

    private PerformanceDO findPerformance(Long performanceId) {
        if (performanceId == null) {
            throw new ClientException("Performance id is required");
        }
        return repository.findPerformance(performanceId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Performance does not exist"));
    }

    private PerformanceDO findPerformanceForUpdate(Long performanceId) {
        if (performanceId == null) {
            throw new ClientException("Performance id is required");
        }
        return repository.findPerformanceForUpdate(performanceId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Performance does not exist"));
    }

    private PerformanceSaleStatusEnum saleStatus(PerformanceDO performance) {
        String value = trimToNull(performance.getSaleStatus());
        if (value == null) {
            return PerformanceSaleStatusEnum.PENDING_SALE;
        }
        try {
            return PerformanceSaleStatusEnum.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ClientException("Unsupported performance sale status: " + value);
        }
    }

    private void assertPerformanceBeforeSale(PerformanceDO performance, String message) {
        if (saleStatus(performance) == PerformanceSaleStatusEnum.ON_SALE) {
            throw new ClientException(message);
        }
    }

    private PerformanceDetailRespDTO startSale(
            Long operatorId,
            PerformanceDO performance,
            LocalDateTime actualSaleTime,
            boolean clearScheduledSaleTime) {
        validatePerformanceReadyForSale(performance, actualSaleTime);
        for (ShowSessionDO show : saleShows(performance)) {
            initConfiguredTicketStock(operatorId, show);
        }
        performance.setSaleStatus(PerformanceSaleStatusEnum.ON_SALE.name());
        if (clearScheduledSaleTime) {
            performance.setScheduledSaleTime(null);
        }
        performance.setActualSaleTime(actualSaleTime);
        performance.setUpdateTime(LocalDateTime.now());
        repository.savePerformance(performance);
        return detail(performance.getId());
    }

    private void validatePerformanceReadyForSale(PerformanceDO performance, LocalDateTime saleStartTime) {
        if (!Integer.valueOf(1).equals(performance.getStatus())) {
            throw new ClientException("Performance must be enabled before sale starts");
        }
        List<ShowSessionDO> shows = saleShows(performance);
        if (shows.isEmpty()) {
            throw new ClientException("Performance must have show sessions before sale starts");
        }
        for (ShowSessionDO show : shows) {
            if (!Integer.valueOf(1).equals(show.getStatus())) {
                throw new ClientException("Show session must be enabled before sale starts");
            }
            if (saleStartTime != null && show.getSaleEndTime() != null && !saleStartTime.isBefore(show.getSaleEndTime())) {
                throw new ClientException("Sale start time must be before show sale end time");
            }
            validateShowTicketConfiguration(show);
        }
    }

    private List<ShowSessionDO> saleShows(PerformanceDO performance) {
        return emptyIfNull(repository.listShowsByPerformance(performance.getId()))
                .stream()
                .filter(this::notDeleted)
                .sorted(Comparator.comparing(ShowSessionDO::getShowTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ShowSessionDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private void validateShowTicketConfiguration(ShowSessionDO show) {
        List<TicketCategoryDO> categories = emptyIfNull(repository.listCategoriesByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .toList();
        if (categories.isEmpty()) {
            throw new ClientException("Show ticket categories are required before sale starts");
        }
        Map<Long, List<ShowSeatCategoryDO>> mappingsByCategory = emptyIfNull(repository.listSeatCategoryMappingsByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .collect(Collectors.groupingBy(ShowSeatCategoryDO::getCategoryId));
        Map<Long, SeatDO> seats = showSeats(show)
                .stream()
                .filter(each -> Integer.valueOf(1).equals(each.getStatus()))
                .collect(Collectors.toMap(SeatDO::getId, Function.identity(), (left, right) -> left));
        Set<Long> configuredSeatIds = new HashSet<>();
        for (TicketCategoryDO category : categories) {
            if (category.getPrice() == null || category.getPrice().signum() <= 0) {
                throw new ClientException("Ticket category price must be positive");
            }
            if (category.getTotalStock() == null || category.getTotalStock() <= 0) {
                throw new ClientException("Ticket category total stock must be positive");
            }
            Integer seatSelectable = normalizeStatus(category.getSeatSelectable());
            List<ShowSeatCategoryDO> mappings = emptyIfNull(mappingsByCategory.get(category.getId()));
            if (Integer.valueOf(1).equals(seatSelectable)) {
                if (mappings.isEmpty()) {
                    throw new ClientException("Selectable ticket category seats are required");
                }
                if (!Objects.equals(category.getTotalStock(), mappings.size())) {
                    throw new ClientException("Selectable ticket category stock must equal assigned seat count");
                }
                Set<Long> categorySeatIds = new HashSet<>();
                for (ShowSeatCategoryDO mapping : mappings) {
                    SeatDO seat = seats.get(mapping.getSeatId());
                    if (seat == null) {
                        throw new ClientException("Configured seat does not exist in show venue");
                    }
                    if (!categorySeatIds.add(mapping.getSeatId()) || !configuredSeatIds.add(mapping.getSeatId())) {
                        throw new ClientException("Seat can only belong to one ticket category in a show");
                    }
                }
            } else if (!mappings.isEmpty()) {
                throw new ClientException("Non-selectable ticket category cannot have assigned seats");
            }
        }
    }

    private void sendSaleStartMessageAfterCommit(PerformanceSaleStartMessage message) {
        if (performanceSaleStartMessageSender == null) {
            return;
        }
        Runnable sendTask = () -> performanceSaleStartMessageSender.send(message);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendTask.run();
            }
        });
    }

    private ArtistDO findArtist(Long artistId) {
        if (artistId == null) {
            throw new ClientException("Artist does not exist");
        }
        return repository.findArtist(artistId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Artist does not exist"));
    }

    private VenueDO findVenue(Long venueId) {
        if (venueId == null) {
            throw new ClientException("Venue does not exist");
        }
        return repository.findVenue(venueId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Venue does not exist"));
    }

    private ShowSessionDO findShow(Long showId) {
        if (showId == null) {
            throw new ClientException("Show id is required");
        }
        return repository.findShow(showId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Show does not exist"));
    }

    private ShowSessionDO findPerformanceShow(Long performanceId, Long showId) {
        ShowSessionDO show = findShow(showId);
        if (!Objects.equals(performanceId, show.getPerformanceId())) {
            throw new ClientException("Show does not belong to performance");
        }
        return show;
    }

    private SeatMapDO defaultSeatMap(Long venueId) {
        HallDO hall = defaultHall(venueId);
        return findSeatMap(hall.getSeatMapId());
    }

    private SeatMapDO existingSeatMap(Long venueId) {
        if (venueId == null) {
            return null;
        }
        return emptyIfNull(repository.listHallsByVenue(venueId))
                .stream()
                .filter(this::notDeleted)
                .filter(each -> each.getSeatMapId() != null)
                .map(each -> repository.findSeatMap(each.getSeatMapId()).filter(this::notDeleted).orElse(null))
                .filter(this::notDeleted)
                .min(Comparator.comparing(SeatMapDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private HallDO defaultHall(Long venueId) {
        VenueDO venue = findVenue(venueId);
        return defaultHall(venue, DEFAULT_SEAT_MAP_ROWS, DEFAULT_SEAT_MAP_COLUMNS);
    }

    private HallDO defaultHall(VenueDO venue, Integer rowCount, Integer columnCount) {
        return emptyIfNull(repository.listHallsByVenue(venue.getId()))
                .stream()
                .filter(this::notDeleted)
                .filter(each -> each.getSeatMapId() != null)
                .filter(each -> repository.findSeatMap(each.getSeatMapId()).filter(this::notDeleted).isPresent())
                .min(Comparator.comparing(HallDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElseGet(() -> createDefaultHall(venue, rowCount, columnCount));
    }

    private HallDO createDefaultHall(VenueDO venue, Integer rowCount, Integer columnCount) {
        LocalDateTime now = LocalDateTime.now();
        int normalizedRows = normalizeSeatLayoutSize(rowCount, DEFAULT_SEAT_MAP_ROWS, "Seat map row count");
        int normalizedColumns = normalizeSeatLayoutSize(columnCount, DEFAULT_SEAT_MAP_COLUMNS, "Seat map column count");
        validateSeatMapCapacity(normalizedRows, normalizedColumns);
        SeatMapDO seatMap = new SeatMapDO();
        seatMap.setId(idGeneratorManager.nextId());
        seatMap.setName(venue.getName() + " 默认座位图");
        seatMap.setRowCount(normalizedRows);
        seatMap.setColumnCount(normalizedColumns);
        seatMap.setCreateTime(now);
        seatMap.setUpdateTime(now);
        seatMap.setDelFlag(0);
        repository.saveSeatMap(seatMap);
        createDefaultSeats(seatMap);

        HallDO hall = new HallDO();
        hall.setId(idGeneratorManager.nextId());
        hall.setVenueId(venue.getId());
        hall.setName("主厅");
        hall.setSeatMapId(seatMap.getId());
        hall.setCreateTime(now);
        hall.setUpdateTime(now);
        hall.setDelFlag(0);
        repository.saveHall(hall);
        return hall;
    }

    private void createDefaultSeats(SeatMapDO seatMap) {
        for (int row = 1; row <= seatMap.getRowCount(); row++) {
            for (int column = 1; column <= seatMap.getColumnCount(); column++) {
                SeatDO seat = new SeatDO();
                seat.setId(idGeneratorManager.nextId());
                seat.setSeatMapId(seatMap.getId());
                seat.setAreaId(defaultSeatAreaId(seatMap.getId(), row));
                seat.setRowNo(row);
                seat.setColumnNo(column);
                seat.setSeatNo(defaultSeatNo(row, column));
                seat.setStatus(1);
                seat.setCreateTime(LocalDateTime.now());
                seat.setUpdateTime(LocalDateTime.now());
                seat.setDelFlag(0);
                repository.saveSeat(seat);
            }
        }
    }

    private void syncVenueSeatLayout(VenueDO venue, VenueCreateReqDTO requestParam) {
        int requestedRows = normalizeSeatLayoutSize(
                requestParam == null ? null : requestParam.getSeatRowCount(),
                DEFAULT_SEAT_MAP_ROWS,
                "Seat map row count"
        );
        int requestedColumns = normalizeSeatLayoutSize(
                requestParam == null ? null : requestParam.getSeatColumnCount(),
                DEFAULT_SEAT_MAP_COLUMNS,
                "Seat map column count"
        );
        HallDO hall = defaultHall(venue, requestedRows, requestedColumns);
        SeatMapDO seatMap = findSeatMap(hall.getSeatMapId());
        int targetRows = requestParam != null && requestParam.getSeatRowCount() != null
                ? requestedRows
                : normalizeSeatLayoutSize(seatMap.getRowCount(), DEFAULT_SEAT_MAP_ROWS, "Seat map row count");
        int targetColumns = requestParam != null && requestParam.getSeatColumnCount() != null
                ? requestedColumns
                : normalizeSeatLayoutSize(seatMap.getColumnCount(), DEFAULT_SEAT_MAP_COLUMNS, "Seat map column count");
        syncSeatMapLayout(seatMap, targetRows, targetColumns);
    }

    private void syncSeatMapLayout(SeatMapDO seatMap, int rowCount, int columnCount) {
        validateSeatMapCapacity(rowCount, columnCount);
        List<SeatDO> existingSeats = emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(this::notDeleted)
                .toList();
        Map<String, SeatDO> existingByPosition = existingSeats.stream()
                .filter(each -> each.getRowNo() != null && each.getColumnNo() != null)
                .collect(Collectors.toMap(each -> seatPositionKey(each.getRowNo(), each.getColumnNo()), Function.identity(), (left, right) -> left));
        LocalDateTime now = LocalDateTime.now();

        for (SeatDO seat : existingSeats) {
            if (seat == null || seat.getRowNo() == null || seat.getColumnNo() == null) {
                continue;
            }
            if (seat.getRowNo() > rowCount || seat.getColumnNo() > columnCount) {
                assertSeatNotConfigured(seat.getId(), "Configured seat outside target layout cannot be removed");
                seat.setDelFlag(1);
                seat.setUpdateTime(now);
                repository.saveSeat(seat);
            }
        }

        seatMap.setRowCount(rowCount);
        seatMap.setColumnCount(columnCount);
        seatMap.setUpdateTime(now);
        repository.saveSeatMap(seatMap);

        for (int row = 1; row <= rowCount; row++) {
            for (int column = 1; column <= columnCount; column++) {
                if (existingByPosition.containsKey(seatPositionKey(row, column))) {
                    continue;
                }
                SeatDO seat = new SeatDO();
                seat.setId(idGeneratorManager.nextId());
                seat.setSeatMapId(seatMap.getId());
                seat.setAreaId(defaultSeatAreaId(seatMap.getId(), row));
                seat.setRowNo(row);
                seat.setColumnNo(column);
                seat.setSeatNo(defaultSeatNo(row, column));
                seat.setStatus(1);
                seat.setCreateTime(now);
                seat.setUpdateTime(now);
                seat.setDelFlag(0);
                repository.saveSeat(seat);
            }
        }
    }

    private int normalizeSeatLayoutSize(Integer value, int defaultValue, String fieldName) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < 1) {
            throw new ClientException(fieldName + " must be positive");
        }
        if (normalized > MAX_SEAT_MAP_ROWS && fieldName.toLowerCase(Locale.ROOT).contains("row")) {
            throw new ClientException("Seat map row count cannot exceed " + MAX_SEAT_MAP_ROWS);
        }
        if (normalized > MAX_SEAT_MAP_COLUMNS && fieldName.toLowerCase(Locale.ROOT).contains("column")) {
            throw new ClientException("Seat map column count cannot exceed " + MAX_SEAT_MAP_COLUMNS);
        }
        return normalized;
    }

    private void validateSeatMapCapacity(int rowCount, int columnCount) {
        if ((long) rowCount * columnCount > MAX_SEAT_MAP_CELLS) {
            throw new ClientException("Seat map capacity cannot exceed " + MAX_SEAT_MAP_CELLS);
        }
    }

    private String seatPositionKey(Integer rowNo, Integer columnNo) {
        return rowNo + ":" + columnNo;
    }

    private Long defaultSeatAreaId(Long seatMapId, int row) {
        return seatMapId * 100 + (row <= 2 ? 1 : 2);
    }

    private String defaultSeatNo(int row, int column) {
        return rowLabel(row) + column;
    }

    private String rowLabel(int row) {
        StringBuilder label = new StringBuilder();
        int current = row;
        while (current > 0) {
            current--;
            label.insert(0, (char) ('A' + current % 26));
            current /= 26;
        }
        return label.toString();
    }

    private List<SeatDO> showSeats(ShowSessionDO show) {
        HallDO hall = findHall(show.getHallId());
        SeatMapDO seatMap = findSeatMap(hall.getSeatMapId());
        return emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(this::notDeleted)
                .toList();
    }

    private HallDO findHall(Long hallId) {
        if (hallId == null) {
            throw new ClientException("Hall does not exist");
        }
        return repository.findHall(hallId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Hall does not exist"));
    }

    private SeatMapDO findSeatMap(Long seatMapId) {
        if (seatMapId == null) {
            throw new ClientException("Seat map does not exist");
        }
        return repository.findSeatMap(seatMapId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Seat map does not exist"));
    }

    private SeatDO findSeat(Long seatId) {
        if (seatId == null) {
            throw new ClientException("Seat does not exist");
        }
        return repository.findSeat(seatId)
                .filter(this::notDeleted)
                .orElseThrow(() -> new ClientException("Seat does not exist"));
    }

    private void assertSeatBelongsToSeatMap(SeatDO seat, SeatMapDO seatMap) {
        if (!Objects.equals(seat.getSeatMapId(), seatMap.getId())) {
            throw new ClientException("Seat does not belong to venue");
        }
    }

    private void assertSeatNotConfigured(Long seatId, String message) {
        boolean configured = emptyIfNull(repository.listSeatCategoryMappingsBySeat(seatId))
                .stream()
                .anyMatch(this::notDeleted);
        if (configured) {
            throw new ClientException(message);
        }
    }

    private void expandSeatMapIfNeeded(SeatMapDO seatMap, SeatDO seat) {
        int rowCount = Math.max(seatMap.getRowCount() == null ? 0 : seatMap.getRowCount(), seat.getRowNo());
        int columnCount = Math.max(seatMap.getColumnCount() == null ? 0 : seatMap.getColumnCount(), seat.getColumnNo());
        if (!Objects.equals(rowCount, seatMap.getRowCount()) || !Objects.equals(columnCount, seatMap.getColumnCount())) {
            seatMap.setRowCount(rowCount);
            seatMap.setColumnCount(columnCount);
            seatMap.setUpdateTime(LocalDateTime.now());
            repository.saveSeatMap(seatMap);
        }
    }

    private List<TicketCategoryConfigRespDTO> ticketCategoryConfigByShow(ShowSessionDO show) {
        Map<Long, List<ShowSeatCategoryDO>> mappingsByCategory = emptyIfNull(repository.listSeatCategoryMappingsByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .collect(Collectors.groupingBy(ShowSeatCategoryDO::getCategoryId));
        Map<Long, SeatDO> seats = showSeats(show)
                .stream()
                .filter(this::notDeleted)
                .collect(Collectors.toMap(SeatDO::getId, Function.identity(), (left, right) -> left));
        return emptyIfNull(repository.listCategoriesByShow(show.getId()))
                .stream()
                .filter(this::notDeleted)
                .map(category -> new TicketCategoryConfigRespDTO(
                        category.getId(),
                        category.getShowId(),
                        category.getCategoryName(),
                        category.getPrice(),
                        category.getTotalStock(),
                        category.getRemainingStock(),
                        category.getSeatSelectable(),
                        TICKET_CATEGORY_STATUS_ENABLED,
                        emptyIfNull(mappingsByCategory.get(category.getId()))
                                .stream()
                                .map(mapping -> seats.get(mapping.getSeatId()))
                                .filter(Objects::nonNull)
                                .map(this::convertSeat)
                                .toList(),
                        emptyIfNull(mappingsByCategory.get(category.getId()))
                                .stream()
                                .filter(mapping -> Integer.valueOf(1).equals(mapping.getSaleLocked()))
                                .map(mapping -> seats.get(mapping.getSeatId()))
                                .filter(Objects::nonNull)
                                .map(this::convertSeat)
                                .toList()
                ))
                .toList();
    }

    private long pageOffset(long current, long size) {
        long pageIndex = current - 1;
        if (pageIndex > Long.MAX_VALUE / size) {
            return Long.MAX_VALUE;
        }
        return pageIndex * size;
    }

    private boolean hasActiveShows(Long performanceId) {
        return emptyIfNull(repository.listShowsByPerformance(performanceId))
                .stream()
                .anyMatch(this::notDeleted);
    }

    private boolean hasActiveCategories(Long showId) {
        return emptyIfNull(repository.listCategoriesByShow(showId))
                .stream()
                .anyMatch(this::notDeleted);
    }

    private boolean notDeleted(BaseDO entity) {
        return entity != null && !Integer.valueOf(1).equals(entity.getDelFlag());
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
        SeatMapDO seatMap = existingSeatMap(venue.getId());
        Integer seatCount = seatMap == null
                ? 0
                : (int) emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                        .stream()
                        .filter(this::notDeleted)
                        .count();
        return new VenueRespDTO(
                venue.getId(),
                venue.getName(),
                venue.getCountry(),
                venue.getProvince(),
                venue.getCity(),
                venue.getDistrict(),
                venue.getTown(),
                venue.getVillage(),
                venue.getStreet(),
                venue.getHouseNumber(),
                venue.getEstate(),
                venue.getBuilding(),
                formatVenueAddress(venue),
                seatMap == null ? null : seatMap.getRowCount(),
                seatMap == null ? null : seatMap.getColumnCount(),
                seatCount
        );
    }

    private String formatVenueAddress(VenueDO venue) {
        String storedAddress = trimToNull(venue.getAddress());
        if (storedAddress != null) {
            return storedAddress;
        }
        return composeVenueAddress(venue);
    }

    private String composeVenueAddress(VenueDO venue) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, venue.getProvince());
        appendAddressPart(builder, venue.getCity());
        appendAddressPart(builder, venue.getDistrict());
        appendAddressPart(builder, venue.getTown());
        appendAddressPart(builder, venue.getVillage());
        appendAddressPart(builder, venue.getStreet());
        appendAddressPart(builder, venue.getHouseNumber());
        appendAddressPart(builder, venue.getEstate());
        appendAddressPart(builder, venue.getBuilding());
        return trimToNull(builder.toString());
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        String part = trimToNull(value);
        if (part == null) {
            return;
        }
        int length = builder.length();
        if (length > 0 && builder.substring(Math.max(0, length - part.length())).equals(part)) {
            return;
        }
        builder.append(part);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ShowSessionRespDTO convertShow(ShowSessionDO show) {
        return new ShowSessionRespDTO(
                show.getId(),
                show.getPerformanceId(),
                show.getHallId(),
                show.getShowTime(),
                show.getDurationMinutes(),
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
                TICKET_CATEGORY_STATUS_ENABLED
        );
    }

    private SeatRespDTO convertSeat(SeatDO seat) {
        return new SeatRespDTO(seat.getId(), seat.getAreaId(), seat.getRowNo(), seat.getColumnNo(), seat.getSeatNo(), seat.getStatus());
    }

    private SeatMapRespDTO convertSeatMap(SeatMapDO seatMap) {
        List<SeatRespDTO> seats = emptyIfNull(repository.listSeatsBySeatMap(seatMap.getId()))
                .stream()
                .filter(this::notDeleted)
                .map(this::convertSeat)
                .toList();
        return new SeatMapRespDTO(seatMap.getId(), seatMap.getName(), seatMap.getRowCount(), seatMap.getColumnCount(), seats);
    }
}
