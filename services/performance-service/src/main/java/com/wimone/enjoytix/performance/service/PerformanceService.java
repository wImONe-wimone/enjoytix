package com.wimone.enjoytix.performance.service;

import com.wimone.enjoytix.framework.convention.page.PageResponse;
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

import java.time.LocalDateTime;
import java.util.List;

public interface PerformanceService {

    PageResponse<PerformanceListRespDTO> pageQuery(PerformancePageQueryReqDTO requestParam);

    PerformanceDetailRespDTO detail(Long performanceId);

    PerformanceDetailRespDTO create(Long operatorId, PerformanceCreateReqDTO requestParam);

    PerformanceDetailRespDTO update(Long performanceId, PerformanceUpdateReqDTO requestParam);

    Boolean delete(Long performanceId);

    PerformanceDetailRespDTO startSaleNow(Long operatorId, Long performanceId);

    PerformanceDetailRespDTO scheduleSale(Long operatorId, Long performanceId, PerformanceSaleScheduleReqDTO requestParam);

    PerformanceDetailRespDTO startScheduledSale(Long operatorId, Long performanceId, LocalDateTime scheduledSaleTime);

    List<ShowSessionRespDTO> adminShows(Long performanceId);

    ShowSessionRespDTO createShow(Long operatorId, Long performanceId, ShowSessionCreateReqDTO requestParam);

    ShowSessionRespDTO updateShow(Long performanceId, Long showId, ShowSessionUpdateReqDTO requestParam);

    Boolean deleteShow(Long performanceId, Long showId);

    List<ArtistRespDTO> artists();

    ArtistRespDTO artist(Long artistId);

    ArtistRespDTO createArtist(ArtistCreateReqDTO requestParam);

    ArtistRespDTO updateArtist(Long artistId, ArtistUpdateReqDTO requestParam);

    Boolean deleteArtist(Long artistId);

    List<VenueRespDTO> venues();

    VenueRespDTO venue(Long venueId);

    VenueRespDTO createVenue(VenueCreateReqDTO requestParam);

    VenueRespDTO updateVenue(Long venueId, VenueUpdateReqDTO requestParam);

    Boolean deleteVenue(Long venueId);

    SeatMapRespDTO venueSeatMap(Long venueId);

    List<SeatRespDTO> venueSeats(Long venueId);

    SeatRespDTO createVenueSeat(Long venueId, SeatCreateReqDTO requestParam);

    SeatRespDTO updateVenueSeat(Long venueId, Long seatId, SeatUpdateReqDTO requestParam);

    Boolean deleteVenueSeat(Long venueId, Long seatId);

    ShowSessionRespDTO show(Long showId);

    List<TicketCategoryRespDTO> ticketCategories(Long showId);

    List<TicketCategoryConfigRespDTO> ticketCategoryConfig(Long showId);

    List<TicketCategoryConfigRespDTO> configureShowTicketCategories(Long operatorId, Long performanceId, Long showId, List<TicketCategoryConfigReqDTO> requestParam);

    SeatMapRespDTO seatMap(Long showId);
}
