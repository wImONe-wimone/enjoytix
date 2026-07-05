package com.wimone.enjoytix.performance.repository;

import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;

import java.util.List;
import java.util.Optional;

public interface PerformanceRepository {

    List<PerformanceDO> listPerformances();

    Optional<PerformanceDO> findPerformance(Long performanceId);

    Optional<ArtistDO> findArtist(Long artistId);

    List<ArtistDO> listArtistsByIds(List<Long> artistIds);

    Optional<VenueDO> findVenue(Long venueId);

    List<VenueDO> listVenuesByIds(List<Long> venueIds);

    Optional<HallDO> findHall(Long hallId);

    Optional<ShowSessionDO> findShow(Long showId);

    List<ShowSessionDO> listShowsByPerformance(Long performanceId);

    List<ShowSessionDO> listShowsByPerformanceIds(List<Long> performanceIds);

    List<TicketCategoryDO> listCategoriesByShow(Long showId);

    Optional<SeatMapDO> findSeatMap(Long seatMapId);

    List<SeatDO> listSeatsBySeatMap(Long seatMapId);
}
