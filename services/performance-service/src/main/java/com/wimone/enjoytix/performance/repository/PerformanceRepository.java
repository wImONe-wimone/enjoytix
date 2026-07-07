package com.wimone.enjoytix.performance.repository;

import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSeatCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository {

    List<PerformanceDO> listPerformances();

    Optional<PerformanceDO> findPerformance(Long performanceId);

    Optional<PerformanceDO> findPerformanceForUpdate(Long performanceId);

    void savePerformance(PerformanceDO performanceDO);

    Optional<ArtistDO> findArtist(Long artistId);

    void saveArtist(ArtistDO artistDO);

    List<ArtistDO> listArtists();

    List<ArtistDO> listArtistsByIds(List<Long> artistIds);

    Optional<VenueDO> findVenue(Long venueId);

    void saveVenue(VenueDO venueDO);

    List<VenueDO> listVenues();

    List<VenueDO> listVenuesByIds(List<Long> venueIds);

    long countPerformancesByArtistId(Long artistId);

    long countPerformancesByVenueId(Long venueId);

    Optional<HallDO> findHall(Long hallId);

    void saveHall(HallDO hallDO);

    List<HallDO> listHallsByVenue(Long venueId);

    Optional<ShowSessionDO> findShow(Long showId);

    void saveShow(ShowSessionDO showSessionDO);

    List<ShowSessionDO> listShowsByPerformance(Long performanceId);

    List<ShowSessionDO> listShowsByPerformanceIds(List<Long> performanceIds);

    List<ShowSessionDO> listShowsByHall(Long hallId);

    List<TicketCategoryDO> listCategoriesByShow(Long showId);

    void saveCategory(TicketCategoryDO categoryDO);

    Optional<TicketCategoryDO> findCategory(Long categoryId);

    Optional<SeatMapDO> findSeatMap(Long seatMapId);

    void saveSeatMap(SeatMapDO seatMapDO);

    Optional<SeatDO> findSeat(Long seatId);

    Optional<SeatDO> findSeatBySeatMapPosition(Long seatMapId, Integer rowNo, Integer columnNo);

    List<SeatDO> listSeatsBySeatMap(Long seatMapId);

    void saveSeat(SeatDO seatDO);

    List<ShowSeatCategoryDO> listSeatCategoryMappingsByShow(Long showId);

    List<ShowSeatCategoryDO> listSeatCategoryMappingsBySeat(Long seatId);

    void saveSeatCategoryMapping(ShowSeatCategoryDO mappingDO);

    void deleteSeatCategoryMappingsByShow(Long showId);
}
