package com.wimone.enjoytix.performance.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;
import com.wimone.enjoytix.performance.dao.mapper.ArtistMapper;
import com.wimone.enjoytix.performance.dao.mapper.HallMapper;
import com.wimone.enjoytix.performance.dao.mapper.PerformanceMapper;
import com.wimone.enjoytix.performance.dao.mapper.SeatMapMapper;
import com.wimone.enjoytix.performance.dao.mapper.SeatMapper;
import com.wimone.enjoytix.performance.dao.mapper.ShowSessionMapper;
import com.wimone.enjoytix.performance.dao.mapper.TicketCategoryMapper;
import com.wimone.enjoytix.performance.dao.mapper.VenueMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisPlusPerformanceRepository implements PerformanceRepository {

    private final ArtistMapper artistMapper;
    private final VenueMapper venueMapper;
    private final HallMapper hallMapper;
    private final PerformanceMapper performanceMapper;
    private final ShowSessionMapper showSessionMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SeatMapMapper seatMapMapper;
    private final SeatMapper seatMapper;

    public MyBatisPlusPerformanceRepository(
            ArtistMapper artistMapper,
            VenueMapper venueMapper,
            HallMapper hallMapper,
            PerformanceMapper performanceMapper,
            ShowSessionMapper showSessionMapper,
            TicketCategoryMapper ticketCategoryMapper,
            SeatMapMapper seatMapMapper,
            SeatMapper seatMapper) {
        this.artistMapper = artistMapper;
        this.venueMapper = venueMapper;
        this.hallMapper = hallMapper;
        this.performanceMapper = performanceMapper;
        this.showSessionMapper = showSessionMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.seatMapMapper = seatMapMapper;
        this.seatMapper = seatMapper;
    }

    @Override
    public List<PerformanceDO> listPerformances() {
        return performanceMapper.selectList(Wrappers.lambdaQuery(PerformanceDO.class)
                .orderByAsc(PerformanceDO::getId));
    }

    @Override
    public Optional<PerformanceDO> findPerformance(Long performanceId) {
        return Optional.ofNullable(performanceMapper.selectById(performanceId));
    }

    @Override
    public Optional<ArtistDO> findArtist(Long artistId) {
        return Optional.ofNullable(artistMapper.selectById(artistId));
    }

    @Override
    public List<ArtistDO> listArtistsByIds(List<Long> artistIds) {
        List<Long> ids = normalizeIds(artistIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return artistMapper.selectList(Wrappers.lambdaQuery(ArtistDO.class)
                .in(ArtistDO::getId, ids));
    }

    @Override
    public Optional<VenueDO> findVenue(Long venueId) {
        return Optional.ofNullable(venueMapper.selectById(venueId));
    }

    @Override
    public List<VenueDO> listVenuesByIds(List<Long> venueIds) {
        List<Long> ids = normalizeIds(venueIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return venueMapper.selectList(Wrappers.lambdaQuery(VenueDO.class)
                .in(VenueDO::getId, ids));
    }

    @Override
    public Optional<HallDO> findHall(Long hallId) {
        return Optional.ofNullable(hallMapper.selectById(hallId));
    }

    @Override
    public Optional<ShowSessionDO> findShow(Long showId) {
        return Optional.ofNullable(showSessionMapper.selectById(showId));
    }

    @Override
    public List<ShowSessionDO> listShowsByPerformance(Long performanceId) {
        return showSessionMapper.selectList(Wrappers.lambdaQuery(ShowSessionDO.class)
                .eq(ShowSessionDO::getPerformanceId, performanceId)
                .orderByAsc(ShowSessionDO::getShowTime));
    }

    @Override
    public List<ShowSessionDO> listShowsByPerformanceIds(List<Long> performanceIds) {
        List<Long> ids = normalizeIds(performanceIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        return showSessionMapper.selectList(Wrappers.lambdaQuery(ShowSessionDO.class)
                .in(ShowSessionDO::getPerformanceId, ids)
                .orderByAsc(ShowSessionDO::getPerformanceId)
                .orderByAsc(ShowSessionDO::getShowTime));
    }

    @Override
    public List<TicketCategoryDO> listCategoriesByShow(Long showId) {
        return ticketCategoryMapper.selectList(Wrappers.lambdaQuery(TicketCategoryDO.class)
                .eq(TicketCategoryDO::getShowId, showId)
                .orderByDesc(TicketCategoryDO::getPrice));
    }

    @Override
    public Optional<SeatMapDO> findSeatMap(Long seatMapId) {
        return Optional.ofNullable(seatMapMapper.selectById(seatMapId));
    }

    @Override
    public List<SeatDO> listSeatsBySeatMap(Long seatMapId) {
        return seatMapper.selectList(Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getSeatMapId, seatMapId)
                .orderByAsc(SeatDO::getRowNo)
                .orderByAsc(SeatDO::getColumnNo));
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
