package com.wimone.enjoytix.performance.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.framework.database.base.BaseDO;
import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSeatCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;
import com.wimone.enjoytix.performance.dao.mapper.ArtistMapper;
import com.wimone.enjoytix.performance.dao.mapper.HallMapper;
import com.wimone.enjoytix.performance.dao.mapper.PerformanceMapper;
import com.wimone.enjoytix.performance.dao.mapper.SeatMapMapper;
import com.wimone.enjoytix.performance.dao.mapper.SeatMapper;
import com.wimone.enjoytix.performance.dao.mapper.ShowSessionMapper;
import com.wimone.enjoytix.performance.dao.mapper.ShowSeatCategoryMapper;
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
    private final ShowSeatCategoryMapper showSeatCategoryMapper;
    private final SeatMapMapper seatMapMapper;
    private final SeatMapper seatMapper;

    public MyBatisPlusPerformanceRepository(
            ArtistMapper artistMapper,
            VenueMapper venueMapper,
            HallMapper hallMapper,
            PerformanceMapper performanceMapper,
            ShowSessionMapper showSessionMapper,
            TicketCategoryMapper ticketCategoryMapper,
            ShowSeatCategoryMapper showSeatCategoryMapper,
            SeatMapMapper seatMapMapper,
            SeatMapper seatMapper) {
        this.artistMapper = artistMapper;
        this.venueMapper = venueMapper;
        this.hallMapper = hallMapper;
        this.performanceMapper = performanceMapper;
        this.showSessionMapper = showSessionMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.showSeatCategoryMapper = showSeatCategoryMapper;
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
    public Optional<PerformanceDO> findPerformanceForUpdate(Long performanceId) {
        return Optional.ofNullable(performanceMapper.selectOne(Wrappers.lambdaQuery(PerformanceDO.class)
                .eq(PerformanceDO::getId, performanceId)
                .last("FOR UPDATE")));
    }

    @Override
    public void savePerformance(PerformanceDO performanceDO) {
        if (softDeleteIfNeeded(performanceDO, performanceMapper)) {
            return;
        }
        if (performanceDO.getId() == null || performanceMapper.selectById(performanceDO.getId()) == null) {
            performanceMapper.insert(performanceDO);
            return;
        }
        performanceMapper.updateById(performanceDO);
    }

    @Override
    public Optional<ArtistDO> findArtist(Long artistId) {
        return Optional.ofNullable(artistMapper.selectById(artistId));
    }

    @Override
    public void saveArtist(ArtistDO artistDO) {
        if (softDeleteIfNeeded(artistDO, artistMapper)) {
            return;
        }
        if (artistDO.getId() == null || artistMapper.selectById(artistDO.getId()) == null) {
            artistMapper.insert(artistDO);
            return;
        }
        artistMapper.updateById(artistDO);
    }

    @Override
    public List<ArtistDO> listArtists() {
        return artistMapper.selectList(Wrappers.lambdaQuery(ArtistDO.class)
                .orderByAsc(ArtistDO::getId));
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
    public void saveVenue(VenueDO venueDO) {
        if (softDeleteIfNeeded(venueDO, venueMapper)) {
            return;
        }
        if (venueDO.getId() == null || venueMapper.selectById(venueDO.getId()) == null) {
            venueMapper.insert(venueDO);
            return;
        }
        venueMapper.updateById(venueDO);
    }

    @Override
    public List<VenueDO> listVenues() {
        return venueMapper.selectList(Wrappers.lambdaQuery(VenueDO.class)
                .orderByAsc(VenueDO::getId));
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
    public long countPerformancesByArtistId(Long artistId) {
        return performanceMapper.selectCount(Wrappers.lambdaQuery(PerformanceDO.class)
                .eq(PerformanceDO::getArtistId, artistId));
    }

    @Override
    public long countPerformancesByVenueId(Long venueId) {
        return performanceMapper.selectCount(Wrappers.lambdaQuery(PerformanceDO.class)
                .eq(PerformanceDO::getVenueId, venueId));
    }

    @Override
    public Optional<HallDO> findHall(Long hallId) {
        return Optional.ofNullable(hallMapper.selectById(hallId));
    }

    @Override
    public void saveHall(HallDO hallDO) {
        if (softDeleteIfNeeded(hallDO, hallMapper)) {
            return;
        }
        if (hallDO.getId() == null || hallMapper.selectById(hallDO.getId()) == null) {
            hallMapper.insert(hallDO);
            return;
        }
        hallMapper.updateById(hallDO);
    }

    @Override
    public List<HallDO> listHallsByVenue(Long venueId) {
        return hallMapper.selectList(Wrappers.lambdaQuery(HallDO.class)
                .eq(HallDO::getVenueId, venueId)
                .orderByAsc(HallDO::getId));
    }

    @Override
    public Optional<ShowSessionDO> findShow(Long showId) {
        return Optional.ofNullable(showSessionMapper.selectById(showId));
    }

    @Override
    public void saveShow(ShowSessionDO showSessionDO) {
        if (softDeleteIfNeeded(showSessionDO, showSessionMapper)) {
            return;
        }
        if (showSessionDO.getId() == null || showSessionMapper.selectById(showSessionDO.getId()) == null) {
            showSessionMapper.insert(showSessionDO);
            return;
        }
        showSessionMapper.updateById(showSessionDO);
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
    public List<ShowSessionDO> listShowsByHall(Long hallId) {
        return showSessionMapper.selectList(Wrappers.lambdaQuery(ShowSessionDO.class)
                .eq(ShowSessionDO::getHallId, hallId)
                .orderByAsc(ShowSessionDO::getShowTime));
    }

    @Override
    public List<TicketCategoryDO> listCategoriesByShow(Long showId) {
        return ticketCategoryMapper.selectList(Wrappers.lambdaQuery(TicketCategoryDO.class)
                .eq(TicketCategoryDO::getShowId, showId)
                .orderByDesc(TicketCategoryDO::getPrice));
    }

    @Override
    public void saveCategory(TicketCategoryDO categoryDO) {
        if (softDeleteIfNeeded(categoryDO, ticketCategoryMapper)) {
            return;
        }
        if (categoryDO.getId() == null || ticketCategoryMapper.selectById(categoryDO.getId()) == null) {
            ticketCategoryMapper.insert(categoryDO);
            return;
        }
        ticketCategoryMapper.updateById(categoryDO);
    }

    @Override
    public Optional<TicketCategoryDO> findCategory(Long categoryId) {
        return Optional.ofNullable(ticketCategoryMapper.selectById(categoryId));
    }

    @Override
    public Optional<SeatMapDO> findSeatMap(Long seatMapId) {
        return Optional.ofNullable(seatMapMapper.selectById(seatMapId));
    }

    @Override
    public void saveSeatMap(SeatMapDO seatMapDO) {
        if (softDeleteIfNeeded(seatMapDO, seatMapMapper)) {
            return;
        }
        if (seatMapDO.getId() == null || seatMapMapper.selectById(seatMapDO.getId()) == null) {
            seatMapMapper.insert(seatMapDO);
            return;
        }
        seatMapMapper.updateById(seatMapDO);
    }

    @Override
    public Optional<SeatDO> findSeat(Long seatId) {
        return Optional.ofNullable(seatMapper.selectById(seatId));
    }

    @Override
    public Optional<SeatDO> findSeatBySeatMapPosition(Long seatMapId, Integer rowNo, Integer columnNo) {
        return Optional.ofNullable(seatMapper.selectOne(Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getSeatMapId, seatMapId)
                .eq(SeatDO::getRowNo, rowNo)
                .eq(SeatDO::getColumnNo, columnNo)
                .last("LIMIT 1")));
    }

    @Override
    public List<SeatDO> listSeatsBySeatMap(Long seatMapId) {
        return seatMapper.selectList(Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getSeatMapId, seatMapId)
                .orderByAsc(SeatDO::getRowNo)
                .orderByAsc(SeatDO::getColumnNo));
    }

    @Override
    public void saveSeat(SeatDO seatDO) {
        if (softDeleteIfNeeded(seatDO, seatMapper)) {
            return;
        }
        if (seatDO.getId() == null || seatMapper.selectById(seatDO.getId()) == null) {
            seatMapper.insert(seatDO);
            return;
        }
        seatMapper.updateById(seatDO);
    }

    @Override
    public List<ShowSeatCategoryDO> listSeatCategoryMappingsByShow(Long showId) {
        return showSeatCategoryMapper.selectList(Wrappers.lambdaQuery(ShowSeatCategoryDO.class)
                .eq(ShowSeatCategoryDO::getShowId, showId)
                .orderByAsc(ShowSeatCategoryDO::getSeatId));
    }

    @Override
    public List<ShowSeatCategoryDO> listSeatCategoryMappingsBySeat(Long seatId) {
        return showSeatCategoryMapper.selectList(Wrappers.lambdaQuery(ShowSeatCategoryDO.class)
                .eq(ShowSeatCategoryDO::getSeatId, seatId)
                .orderByAsc(ShowSeatCategoryDO::getShowId));
    }

    @Override
    public void saveSeatCategoryMapping(ShowSeatCategoryDO mappingDO) {
        if (softDeleteIfNeeded(mappingDO, showSeatCategoryMapper)) {
            return;
        }
        if (mappingDO.getId() == null || showSeatCategoryMapper.selectById(mappingDO.getId()) == null) {
            showSeatCategoryMapper.insert(mappingDO);
            return;
        }
        showSeatCategoryMapper.updateById(mappingDO);
    }

    @Override
    public void deleteSeatCategoryMappingsByShow(Long showId) {
        showSeatCategoryMapper.deletePhysicallyByShowId(showId);
    }

    private <T extends BaseDO> boolean softDeleteIfNeeded(T entity, BaseMapper<T> mapper) {
        if (!Integer.valueOf(1).equals(entity.getDelFlag())) {
            return false;
        }
        if (entity.getId() != null) {
            mapper.deleteById(entity.getId());
        }
        return true;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
