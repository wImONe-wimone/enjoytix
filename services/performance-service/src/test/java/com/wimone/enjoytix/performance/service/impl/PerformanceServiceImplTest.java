package com.wimone.enjoytix.performance.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator;
import com.wimone.enjoytix.performance.common.enums.PerformanceSaleStatusEnum;
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
import com.wimone.enjoytix.performance.message.PerformanceSaleStartMessage;
import com.wimone.enjoytix.performance.message.PerformanceSaleStartMessageSender;
import com.wimone.enjoytix.performance.remote.TicketRemoteService;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockConfigInitReqDTO;
import com.wimone.enjoytix.performance.remote.dto.TicketShowStockInitReqDTO;
import com.wimone.enjoytix.performance.repository.InMemoryPerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceServiceImplTest {

    private PerformanceServiceImpl performanceService;

    @BeforeEach
    void setUp() {
        InMemoryPerformanceRepository repository = new InMemoryPerformanceRepository();
        repository.initSeedData();
        performanceService = new PerformanceServiceImpl(repository, new IdGeneratorManager(new SnowflakeIdGenerator(1)));
    }

    @Test
    void pageQueryShouldReturnSortedPerformances() {
        PageResponse<PerformanceListRespDTO> page = performanceService.pageQuery(new PerformancePageQueryReqDTO());

        assertEquals(1, page.getCurrent());
        assertEquals(20, page.getSize());
        assertEquals(2, page.getTotal());
        assertEquals(1001L, page.getRecords().get(0).performanceId());
        assertEquals(1002L, page.getRecords().get(1).performanceId());
    }

    @Test
    void pageQueryShouldHandleNullRequest() {
        PageResponse<PerformanceListRespDTO> page = performanceService.pageQuery(null);

        assertEquals(1, page.getCurrent());
        assertEquals(20, page.getSize());
        assertEquals(2, page.getTotal());
    }

    @Test
    void pageQueryShouldFilterByTypeArtistVenueAndDate() {
        PerformancePageQueryReqDTO query = new PerformancePageQueryReqDTO();
        query.setPerformanceType("concert");
        query.setArtistName("aurora");
        query.setVenueName("arena");
        query.setShowDate(LocalDate.of(2026, 8, 16));

        PageResponse<PerformanceListRespDTO> page = performanceService.pageQuery(query);

        assertEquals(1, page.getTotal());
        assertEquals("Aurora Band 2026 Live", page.getRecords().get(0).title());
    }

    @Test
    void pageQueryShouldReturnEmptyRecordsForOutOfRangePage() {
        PerformancePageQueryReqDTO query = new PerformancePageQueryReqDTO();
        query.setCurrent(99);
        query.setSize(1);

        PageResponse<PerformanceListRespDTO> page = performanceService.pageQuery(query);

        assertEquals(2, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void pageQueryShouldReturnEmptyRecordsWhenPageOffsetOverflows() {
        PerformancePageQueryReqDTO query = new PerformancePageQueryReqDTO();
        query.setCurrent(Long.MAX_VALUE);
        query.setSize(200);

        PageResponse<PerformanceListRespDTO> page = performanceService.pageQuery(query);

        assertEquals(Long.MAX_VALUE, page.getCurrent());
        assertEquals(200, page.getSize());
        assertEquals(2, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void detailShouldIncludeVenueArtistAndSessions() {
        PerformanceDetailRespDTO detail = performanceService.detail(1001L);

        assertEquals("Aurora Band 2026 Live", detail.title());
        assertNotNull(detail.artist());
        assertNotNull(detail.venue());
        assertEquals("中国", detail.venue().country());
        assertEquals("北京市", detail.venue().city());
        assertEquals("朝阳区", detail.venue().district());
        assertEquals("阜通东大街", detail.venue().street());
        assertEquals("6号", detail.venue().houseNumber());
        assertEquals("北京市朝阳区阜通东大街6号", detail.venue().address());
        assertFalse(detail.sessions().isEmpty());
        assertEquals(120, detail.sessions().get(0).durationMinutes());
    }

    @Test
    void showRelatedApisShouldReturnCategoryAndSeatMapData() {
        List<TicketCategoryRespDTO> categories = performanceService.ticketCategories(2001L);
        SeatMapRespDTO seatMap = performanceService.seatMap(2001L);

        assertEquals(3, categories.size());
        assertEquals("VIP", categories.get(0).categoryName());
        assertEquals(400L, seatMap.seatMapId());
        assertEquals(48, seatMap.seats().size());
    }

    @Test
    void missingPerformanceShouldThrowClientException() {
        assertThrows(ClientException.class, () -> performanceService.detail(999999L));
    }

    @Test
    void adminShouldCreateUpdateAndDeletePerformance() {
        PerformanceCreateReqDTO createReq = new PerformanceCreateReqDTO();
        createReq.setTitle("Gallery Summer Exhibition");
        createReq.setPerformanceType("EXHIBITION");
        createReq.setArtistId(100L);
        createReq.setVenueId(200L);
        createReq.setPosterUrl("https://static.enjoytix.local/posters/gallery.jpg");
        createReq.setDescription("Admin managed exhibition");
        createReq.setStatus(1);

        PerformanceDetailRespDTO created = performanceService.create(createReq);

        assertNotNull(created.performanceId());
        assertEquals("Gallery Summer Exhibition", created.title());
        assertEquals("北京市", created.city());
        assertEquals(3, performanceService.pageQuery(new PerformancePageQueryReqDTO()).getTotal());

        PerformanceUpdateReqDTO updateReq = new PerformanceUpdateReqDTO();
        updateReq.setTitle("Gallery Autumn Exhibition");
        updateReq.setPerformanceType("EXHIBITION");
        updateReq.setArtistId(101L);
        updateReq.setVenueId(201L);
        updateReq.setPosterUrl("");
        updateReq.setDescription("Updated by admin");
        updateReq.setStatus(0);

        PerformanceDetailRespDTO updated = performanceService.update(created.performanceId(), updateReq);

        assertEquals("Gallery Autumn Exhibition", updated.title());
        assertEquals("North Theatre", updated.artist().name());
        assertEquals(0, updated.status());

        assertTrue(performanceService.delete(created.performanceId()));
        assertThrows(ClientException.class, () -> performanceService.detail(created.performanceId()));
        PageResponse<PerformanceListRespDTO> afterDeletePage = performanceService.pageQuery(new PerformancePageQueryReqDTO());
        assertEquals(2, afterDeletePage.getTotal());
        assertTrue(afterDeletePage.getRecords().stream().noneMatch(each -> created.performanceId().equals(each.performanceId())));
    }

    @Test
    void adminShouldRejectInvalidTypeAndListFormOptions() {
        assertEquals(2, performanceService.artists().size());
        assertEquals(2, performanceService.venues().size());

        PerformanceCreateReqDTO request = new PerformanceCreateReqDTO();
        request.setTitle("Invalid Type Project");
        request.setPerformanceType("UNKNOWN");
        request.setArtistId(100L);
        request.setVenueId(200L);

        assertThrows(ClientException.class, () -> performanceService.create(request));
    }

    @Test
    void adminShouldCreateUpdateAndDeleteArtist() {
        ArtistCreateReqDTO createReq = new ArtistCreateReqDTO();
        createReq.setName("Solo Unit");
        createReq.setDescription("Independent artist");

        ArtistRespDTO created = performanceService.createArtist(createReq);

        assertNotNull(created.artistId());
        assertEquals("Solo Unit", created.name());
        assertEquals(3, performanceService.artists().size());

        ArtistUpdateReqDTO updateReq = new ArtistUpdateReqDTO();
        updateReq.setName("Solo Unit Updated");
        updateReq.setDescription("");

        ArtistRespDTO updated = performanceService.updateArtist(created.artistId(), updateReq);

        assertEquals("Solo Unit Updated", updated.name());
        assertEquals(null, updated.description());
        assertTrue(performanceService.deleteArtist(created.artistId()));
        assertThrows(ClientException.class, () -> performanceService.artist(created.artistId()));
        assertTrue(performanceService.artists().stream().noneMatch(each -> created.artistId().equals(each.artistId())));
        assertThrows(ClientException.class, () -> performanceService.deleteArtist(100L));
    }

    @Test
    void adminShouldCreateUpdateAndDeleteVenueWithDefaultHall() {
        VenueRespDTO created = performanceService.createVenue(venueCreateReq("New Arena", "West Road", "88"));

        assertNotNull(created.venueId());
        assertEquals("New Arena", created.name());
        assertEquals("Test City", created.city());
        assertEquals(10, created.seatRowCount());
        assertEquals(10, created.seatColumnCount());
        assertEquals(100, created.seatCount());
        assertEquals(3, performanceService.venues().size());

        VenueUpdateReqDTO updateReq = new VenueUpdateReqDTO();
        updateReq.setName("New Arena Updated");
        updateReq.setCountry("China");
        updateReq.setProvince("Test Province");
        updateReq.setCity("Test City");
        updateReq.setDistrict("Test District");
        updateReq.setStreet("East Road");
        updateReq.setHouseNumber("99");

        VenueRespDTO updated = performanceService.updateVenue(created.venueId(), updateReq);

        assertEquals("New Arena Updated", updated.name());
        assertTrue(updated.address().contains("East Road"));

        PerformanceCreateReqDTO performanceReq = new PerformanceCreateReqDTO();
        performanceReq.setTitle("Default Hall Show");
        performanceReq.setPerformanceType("CONCERT");
        performanceReq.setArtistId(100L);
        performanceReq.setVenueId(created.venueId());
        PerformanceDetailRespDTO performance = performanceService.create(performanceReq);

        ShowSessionCreateReqDTO showReq = new ShowSessionCreateReqDTO();
        showReq.setShowTime(LocalDateTime.of(2026, 10, 1, 19, 30));
        showReq.setDurationMinutes(90);

        ShowSessionRespDTO show = performanceService.createShow(1L, performance.performanceId(), showReq);

        assertNotNull(show.hallId());
        assertEquals(100, performanceService.seatMap(show.showId()).seats().size());
        assertThrows(ClientException.class, () -> performanceService.delete(performance.performanceId()));
        assertThrows(ClientException.class, () -> performanceService.deleteVenue(created.venueId()));

        assertTrue(performanceService.deleteShow(performance.performanceId(), show.showId()));
        assertTrue(performanceService.delete(performance.performanceId()));
        assertTrue(performanceService.deleteVenue(created.venueId()));
        assertThrows(ClientException.class, () -> performanceService.venue(created.venueId()));
        assertTrue(performanceService.venues().stream().noneMatch(each -> created.venueId().equals(each.venueId())));
    }

    @Test
    void adminShouldGenerateVenueSeatMapFromConfiguredLayout() {
        VenueCreateReqDTO createReq = venueCreateReq("Layout Hall", "Grid Road", "12");
        createReq.setSeatRowCount(3);
        createReq.setSeatColumnCount(4);

        VenueRespDTO created = performanceService.createVenue(createReq);

        assertEquals(3, created.seatRowCount());
        assertEquals(4, created.seatColumnCount());
        assertEquals(12, created.seatCount());
        assertEquals(12, performanceService.venueSeats(created.venueId()).size());

        VenueUpdateReqDTO expandReq = venueUpdateReq("Layout Hall", "Grid Road", "12");
        expandReq.setSeatRowCount(4);
        expandReq.setSeatColumnCount(5);
        VenueRespDTO expanded = performanceService.updateVenue(created.venueId(), expandReq);

        assertEquals(4, expanded.seatRowCount());
        assertEquals(5, expanded.seatColumnCount());
        assertEquals(20, expanded.seatCount());
        assertEquals(20, performanceService.venueSeats(created.venueId()).size());

        VenueUpdateReqDTO shrinkReq = venueUpdateReq("Layout Hall", "Grid Road", "12");
        shrinkReq.setSeatRowCount(2);
        shrinkReq.setSeatColumnCount(3);
        VenueRespDTO shrunk = performanceService.updateVenue(created.venueId(), shrinkReq);

        assertEquals(2, shrunk.seatRowCount());
        assertEquals(3, shrunk.seatColumnCount());
        assertEquals(6, shrunk.seatCount());
        assertEquals(6, performanceService.venueSeats(created.venueId()).size());
    }

    @Test
    void adminShouldRejectShrinkingLayoutWhenConfiguredSeatsWouldBeRemoved() {
        VenueUpdateReqDTO updateReq = venueUpdateReq("Enjoy Arena", "阜通东大街", "6号");
        updateReq.setCountry("中国");
        updateReq.setProvince("北京市");
        updateReq.setCity("北京市");
        updateReq.setDistrict("朝阳区");
        updateReq.setSeatRowCount(1);
        updateReq.setSeatColumnCount(1);

        assertThrows(ClientException.class, () -> performanceService.updateVenue(200L, updateReq));
    }

    @Test
    void adminShouldRejectVenueChangeAfterShowsAreCreated() {
        PerformanceUpdateReqDTO updateReq = new PerformanceUpdateReqDTO();
        updateReq.setTitle("Aurora Band 2026 Live");
        updateReq.setPerformanceType("CONCERT");
        updateReq.setArtistId(100L);
        updateReq.setVenueId(201L);
        updateReq.setStatus(1);

        assertThrows(ClientException.class, () -> performanceService.update(1001L, updateReq));
    }

    @Test
    void adminShouldRejectShowTimeConflictInSameHall() {
        PerformanceDetailRespDTO performance = createPendingPerformance(200L, "Conflict Pending Performance");
        ShowSessionCreateReqDTO createReq = new ShowSessionCreateReqDTO();
        createReq.setShowTime(LocalDateTime.of(2026, 8, 16, 20, 0));
        createReq.setDurationMinutes(60);

        assertThrows(ClientException.class, () -> performanceService.createShow(1L, performance.performanceId(), createReq));
    }

    @Test
    void adminShouldCreateUpdateAndDeleteShowSession() {
        PerformanceDetailRespDTO performance = createPendingPerformanceWithConfiguredShow(200L);
        assertEquals(1, performanceService.adminShows(performance.performanceId()).size());

        ShowSessionCreateReqDTO createReq = new ShowSessionCreateReqDTO();
        createReq.setShowTime(LocalDateTime.of(2026, 8, 18, 19, 30));
        createReq.setDurationMinutes(135);
        createReq.setStatus(1);

        ShowSessionRespDTO created = performanceService.createShow(1L, performance.performanceId(), createReq);

        assertNotNull(created.showId());
        assertEquals(performance.performanceId(), created.performanceId());
        assertEquals(300L, created.hallId());
        assertEquals(135, created.durationMinutes());
        assertEquals(2, performanceService.adminShows(performance.performanceId()).size());
        List<TicketCategoryRespDTO> copiedCategories = performanceService.ticketCategories(created.showId());
        assertEquals(2, copiedCategories.size());
        assertEquals(created.showId(), copiedCategories.get(0).showId());

        ShowSessionUpdateReqDTO updateReq = new ShowSessionUpdateReqDTO();
        updateReq.setShowTime(LocalDateTime.of(2026, 8, 19, 20, 0));
        updateReq.setDurationMinutes(150);
        updateReq.setStatus(0);

        ShowSessionRespDTO updated = performanceService.updateShow(performance.performanceId(), created.showId(), updateReq);

        assertEquals(LocalDateTime.of(2026, 8, 19, 20, 0), updated.showTime());
        assertEquals(150, updated.durationMinutes());
        assertEquals(0, updated.status());
        assertThrows(ClientException.class, () -> performanceService.updateShow(1002L, created.showId(), updateReq));

        assertTrue(performanceService.deleteShow(performance.performanceId(), created.showId()));
        assertEquals(1, performanceService.adminShows(performance.performanceId()).size());
        assertThrows(ClientException.class, () -> performanceService.show(created.showId()));
        assertThrows(ClientException.class, () -> performanceService.ticketCategories(created.showId()));
        assertThrows(ClientException.class, () -> performanceService.ticketCategoryConfig(created.showId()));
        assertThrows(ClientException.class, () -> performanceService.seatMap(created.showId()));
    }

    @Test
    void adminShouldCreateUpdateAndDeleteVenueSeat() {
        assertEquals(48, performanceService.venueSeats(200L).size());

        SeatRespDTO created = performanceService.createVenueSeat(200L, seatReq(40003L, 7, 1, "G1", 1));

        assertNotNull(created.seatId());
        assertEquals(40003L, created.areaId());
        assertEquals(49, performanceService.venueSeats(200L).size());
        assertThrows(ClientException.class, () -> performanceService.createVenueSeat(200L, seatReq(40004L, 7, 1, "G9", 1)));
        assertThrows(ClientException.class, () -> performanceService.createVenueSeat(200L, seatReq(40004L, 7, 2, "g1", 1)));

        SeatUpdateReqDTO updateReq = seatUpdateReq(40005L, 7, 2, "G2", 0);
        SeatRespDTO updated = performanceService.updateVenueSeat(200L, created.seatId(), updateReq);

        assertEquals(40005L, updated.areaId());
        assertEquals(7, updated.rowNo());
        assertEquals(2, updated.columnNo());
        assertEquals("G2", updated.seatNo());
        assertEquals(0, updated.status());

        assertTrue(performanceService.deleteVenueSeat(200L, created.seatId()));
        assertEquals(48, performanceService.venueSeats(200L).size());
        assertTrue(performanceService.venueSeats(200L).stream().noneMatch(each -> created.seatId().equals(each.seatId())));
        assertThrows(ClientException.class, () -> performanceService.updateVenueSeat(200L, created.seatId(), updateReq));
    }

    @Test
    void adminShouldRejectChangingConfiguredVenueSeat() {
        SeatUpdateReqDTO updateReq = seatUpdateReq(40001L, 1, 1, "A1", 1);

        assertThrows(ClientException.class, () -> performanceService.updateVenueSeat(200L, 400101L, updateReq));
        assertThrows(ClientException.class, () -> performanceService.deleteVenueSeat(200L, 400101L));
    }

    @Test
    void adminVenueSeatMapShouldExposeDefaultSeatMap() {
        SeatMapRespDTO seatMap = performanceService.venueSeatMap(200L);

        assertEquals(400L, seatMap.seatMapId());
        assertEquals(6, seatMap.rowCount());
        assertEquals(8, seatMap.columnCount());
        assertEquals(48, seatMap.seats().size());
    }

    @Test
    void createShowShouldPersistExplicitTicketCategorySeatConfig() {
        PerformanceDetailRespDTO performance = createPendingPerformance(200L, "Explicit Ticket Config Performance");
        ShowSessionCreateReqDTO createReq = showReq(LocalDateTime.of(2026, 8, 17, 21, 0));
        TicketCategoryConfigReqDTO vipReq = ticketCategoryReq("Config VIP", "990.00", 2, 1, List.of(400101L, 400102L));
        vipReq.setSeatMapId(400L);
        createReq.setTicketCategories(List.of(
                vipReq,
                ticketCategoryReq("Standing", "199.00", 20, 0, null)
        ));

        ShowSessionRespDTO show = performanceService.createShow(1L, performance.performanceId(), createReq);

        List<TicketCategoryConfigRespDTO> configs = performanceService.ticketCategoryConfig(show.showId());
        assertEquals(2, configs.size());
        TicketCategoryConfigRespDTO vip = configs.stream()
                .filter(each -> "Config VIP".equals(each.categoryName()))
                .findFirst()
                .orElseThrow();
        TicketCategoryConfigRespDTO standing = configs.stream()
                .filter(each -> "Standing".equals(each.categoryName()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, vip.seats().size());
        assertTrue(vip.seats().stream().anyMatch(each -> 400101L == each.seatId()));
        assertTrue(vip.seats().stream().anyMatch(each -> 400102L == each.seatId()));
        assertEquals(0, standing.seats().size());
    }

    @Test
    void createShowShouldRejectTicketCategorySeatMapMismatch() {
        PerformanceDetailRespDTO performance = createPendingPerformance(200L, "Seat Map Mismatch Performance");
        ShowSessionCreateReqDTO createReq = showReq(LocalDateTime.of(2026, 8, 18, 21, 0));
        TicketCategoryConfigReqDTO vipReq = ticketCategoryReq("Config VIP", "990.00", 1, 1, List.of(400101L));
        vipReq.setSeatMapId(401L);
        createReq.setTicketCategories(List.of(vipReq));

        ClientException exception = assertThrows(ClientException.class, () -> performanceService.createShow(1L, performance.performanceId(), createReq));

        assertTrue(exception.getMessage().contains("seat map does not match"));
    }

    @Test
    void configureShowTicketCategoriesShouldPersistForEmptyShow() {
        ShowSessionRespDTO show = createEmptyShow(201L, LocalDateTime.of(2026, 11, 1, 19, 30));

        List<TicketCategoryConfigRespDTO> configs = performanceService.configureShowTicketCategories(
                1L,
                show.performanceId(),
                show.showId(),
                List.of(ticketCategoryReq("Standard", "280.00", 2, 1, List.of(401101L, 401102L)))
        );

        assertEquals(1, configs.size());
        assertEquals("Standard", configs.get(0).categoryName());
        assertEquals(2, configs.get(0).seats().size());
        List<TicketCategoryConfigRespDTO> updatedConfigs = performanceService.configureShowTicketCategories(
                1L,
                show.performanceId(),
                show.showId(),
                List.of(ticketCategoryReq("Second", "180.00", 1, 1, List.of(401103L)))
        );
        assertEquals(1, updatedConfigs.size());
        assertEquals("Second", updatedConfigs.get(0).categoryName());
        assertEquals(1, updatedConfigs.get(0).seats().size());
    }

    @Test
    void configureShowTicketCategoriesShouldRejectInvalidSeatConfigs() {
        ShowSessionRespDTO duplicateNameShow = createEmptyShow(201L, LocalDateTime.of(2026, 11, 2, 19, 30));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                duplicateNameShow.performanceId(),
                duplicateNameShow.showId(),
                List.of(
                        ticketCategoryReq("VIP", "580.00", 1, 1, List.of(401101L)),
                        ticketCategoryReq("vip", "480.00", 1, 1, List.of(401102L))
                )
        ));

        ShowSessionRespDTO duplicateSeatShow = createEmptyShow(201L, LocalDateTime.of(2026, 11, 3, 19, 30));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                duplicateSeatShow.performanceId(),
                duplicateSeatShow.showId(),
                List.of(
                        ticketCategoryReq("VIP", "580.00", 1, 1, List.of(401101L)),
                        ticketCategoryReq("A Zone", "480.00", 1, 1, List.of(401101L))
                )
        ));

        ShowSessionRespDTO duplicateSeatInSameCategoryShow = createEmptyShow(201L, LocalDateTime.of(2026, 11, 4, 19, 30));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                duplicateSeatInSameCategoryShow.performanceId(),
                duplicateSeatInSameCategoryShow.showId(),
                List.of(ticketCategoryReq("VIP", "580.00", 2, 1, List.of(401101L, 401101L)))
        ));

        ShowSessionRespDTO stockMismatchShow = createEmptyShow(201L, LocalDateTime.of(2026, 11, 5, 19, 30));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                stockMismatchShow.performanceId(),
                stockMismatchShow.showId(),
                List.of(ticketCategoryReq("VIP", "580.00", 2, 1, List.of(401101L)))
        ));

        ShowSessionRespDTO wrongVenueSeatShow = createEmptyShow(201L, LocalDateTime.of(2026, 11, 6, 19, 30));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                wrongVenueSeatShow.performanceId(),
                wrongVenueSeatShow.showId(),
                List.of(ticketCategoryReq("VIP", "580.00", 1, 1, List.of(400101L)))
        ));
    }

    @Test
    void createPerformanceShouldDefaultToPendingSale() {
        PerformanceDetailRespDTO created = createPendingPerformance(200L, "Default Pending Sale Performance");

        assertEquals(PerformanceSaleStatusEnum.PENDING_SALE.name(), created.saleStatus());
        assertEquals(null, created.scheduledSaleTime());
        assertEquals(null, created.actualSaleTime());
    }

    @Test
    void configureShowTicketCategoriesShouldPersistLockedSeatsBeforeSale() {
        ShowSessionRespDTO show = createEmptyShow(201L, LocalDateTime.of(2026, 11, 7, 19, 30));
        TicketCategoryConfigReqDTO request = ticketCategoryReq("Lockable", "380.00", 2, 1, List.of(401101L, 401102L));
        request.setLockedSeatIds(List.of(401101L));

        List<TicketCategoryConfigRespDTO> configs = performanceService.configureShowTicketCategories(
                1L,
                show.performanceId(),
                show.showId(),
                List.of(request)
        );

        assertEquals(1, configs.size());
        assertEquals(2, configs.get(0).seats().size());
        assertEquals(1, configs.get(0).lockedSeats().size());
        assertEquals(401101L, configs.get(0).lockedSeats().get(0).seatId());
    }

    @Test
    void ticketCategoryStatusShouldNotControlSeatSaleAvailability() {
        CapturingTicketRemoteService remoteService = new CapturingTicketRemoteService();
        resetService(remoteService, new CapturingSaleStartMessageSender());
        PerformanceDetailRespDTO performance = createPendingPerformance(201L, "Ticket Status Ignored Performance");
        ShowSessionRespDTO show = createEmptyShowUnderPerformance(performance.performanceId(), LocalDateTime.of(2026, 11, 8, 16, 30));
        TicketCategoryConfigReqDTO request = ticketCategoryReq("Status Ignored", "380.00", 2, 1, List.of(401101L, 401102L));
        request.setStatus(0);
        request.setLockedSeatIds(List.of(401102L));

        List<TicketCategoryConfigRespDTO> configs = performanceService.configureShowTicketCategories(
                1L,
                performance.performanceId(),
                show.showId(),
                List.of(request)
        );
        PerformanceDetailRespDTO onSale = performanceService.startSaleNow(1L, performance.performanceId());

        assertEquals(PerformanceSaleStatusEnum.ON_SALE.name(), onSale.saleStatus());
        assertEquals(1, configs.get(0).status());
        assertEquals(1, remoteService.configRequests.size());
        TicketShowStockConfigInitReqDTO initRequest = remoteService.configRequests.get(0);
        assertTrue(initRequest.categories().get(0).seats().stream().anyMatch(each ->
                401101L == each.seatId() && !Boolean.TRUE.equals(each.locked())));
        assertTrue(initRequest.categories().get(0).seats().stream().anyMatch(each ->
                401102L == each.seatId() && Boolean.TRUE.equals(each.locked())));
    }

    @Test
    void configureShowTicketCategoriesShouldReplaceExistingSeatMappings() {
        ShowSessionRespDTO show = createEmptyShow(201L, LocalDateTime.of(2026, 11, 8, 18, 30));
        TicketCategoryConfigReqDTO firstRequest = ticketCategoryReq("First", "380.00", 1, 1, List.of(401101L));
        firstRequest.setLockedSeatIds(List.of(401101L));
        performanceService.configureShowTicketCategories(
                1L,
                show.performanceId(),
                show.showId(),
                List.of(firstRequest)
        );

        List<TicketCategoryConfigRespDTO> configs = performanceService.configureShowTicketCategories(
                1L,
                show.performanceId(),
                show.showId(),
                List.of(ticketCategoryReq("Second", "420.00", 1, 1, List.of(401101L)))
        );

        assertEquals(1, configs.size());
        assertEquals("Second", configs.get(0).categoryName());
        assertEquals(1, configs.get(0).seats().size());
        assertEquals(401101L, configs.get(0).seats().get(0).seatId());
        assertTrue(configs.get(0).lockedSeats().isEmpty());
    }

    @Test
    void startSaleNowShouldInitializeConfiguredStockAndRejectFurtherTicketEdits() {
        CapturingTicketRemoteService remoteService = new CapturingTicketRemoteService();
        resetService(remoteService, new CapturingSaleStartMessageSender());
        PerformanceDetailRespDTO performance = createPendingPerformance(201L, "Start Now Performance");
        ShowSessionRespDTO show = createEmptyShowUnderPerformance(performance.performanceId(), LocalDateTime.of(2026, 11, 9, 19, 30));
        TicketCategoryConfigReqDTO request = ticketCategoryReq("VIP", "680.00", 2, 1, List.of(401101L, 401102L));
        request.setLockedSeatIds(List.of(401101L));
        performanceService.configureShowTicketCategories(1L, performance.performanceId(), show.showId(), List.of(request));

        PerformanceDetailRespDTO onSale = performanceService.startSaleNow(1L, performance.performanceId());

        assertEquals(PerformanceSaleStatusEnum.ON_SALE.name(), onSale.saleStatus());
        assertNotNull(onSale.actualSaleTime());
        assertEquals(1, remoteService.configRequests.size());
        TicketShowStockConfigInitReqDTO initRequest = remoteService.configRequests.get(0);
        assertEquals(show.showId(), initRequest.showId());
        assertTrue(initRequest.categories().get(0).seats().stream().anyMatch(each ->
                401101L == each.seatId() && Boolean.TRUE.equals(each.locked())));
        assertTrue(initRequest.categories().get(0).seats().stream().anyMatch(each ->
                401102L == each.seatId() && !Boolean.TRUE.equals(each.locked())));
        assertThrows(ClientException.class, () -> performanceService.configureShowTicketCategories(
                1L,
                performance.performanceId(),
                show.showId(),
                List.of(ticketCategoryReq("After Sale", "580.00", 1, 1, List.of(401103L)))
        ));
    }

    @Test
    void scheduleSaleShouldSetScheduledStatusAndSendDelayedMessage() {
        CapturingTicketRemoteService remoteService = new CapturingTicketRemoteService();
        CapturingSaleStartMessageSender sender = new CapturingSaleStartMessageSender();
        resetService(remoteService, sender);
        PerformanceDetailRespDTO performance = createPendingPerformance(201L, "Scheduled Sale Performance");
        ShowSessionRespDTO show = createEmptyShowUnderPerformance(performance.performanceId(), LocalDateTime.of(2026, 11, 9, 19, 30));
        performanceService.configureShowTicketCategories(
                1L,
                performance.performanceId(),
                show.showId(),
                List.of(ticketCategoryReq("Standard", "280.00", 2, 1, List.of(401101L, 401102L)))
        );
        PerformanceSaleScheduleReqDTO request = new PerformanceSaleScheduleReqDTO();
        request.setSaleStartTime(LocalDateTime.now().plusMinutes(5));

        PerformanceDetailRespDTO scheduled = performanceService.scheduleSale(1L, performance.performanceId(), request);

        assertEquals(PerformanceSaleStatusEnum.SCHEDULED.name(), scheduled.saleStatus());
        assertNotNull(scheduled.scheduledSaleTime());
        assertEquals(1, sender.messages.size());
        assertEquals(performance.performanceId(), sender.messages.get(0).performanceId());
        assertTrue(remoteService.configRequests.isEmpty());
    }

    private VenueCreateReqDTO venueCreateReq(String name, String street, String houseNumber) {
        VenueCreateReqDTO request = new VenueCreateReqDTO();
        request.setName(name);
        request.setCountry("China");
        request.setProvince("Test Province");
        request.setCity("Test City");
        request.setDistrict("Test District");
        request.setStreet(street);
        request.setHouseNumber(houseNumber);
        return request;
    }

    private VenueUpdateReqDTO venueUpdateReq(String name, String street, String houseNumber) {
        VenueUpdateReqDTO request = new VenueUpdateReqDTO();
        request.setName(name);
        request.setCountry("China");
        request.setProvince("Test Province");
        request.setCity("Test City");
        request.setDistrict("Test District");
        request.setStreet(street);
        request.setHouseNumber(houseNumber);
        return request;
    }

    private SeatCreateReqDTO seatReq(Long areaId, int rowNo, int columnNo, String seatNo, int status) {
        SeatCreateReqDTO request = new SeatCreateReqDTO();
        request.setAreaId(areaId);
        request.setRowNo(rowNo);
        request.setColumnNo(columnNo);
        request.setSeatNo(seatNo);
        request.setStatus(status);
        return request;
    }

    private SeatUpdateReqDTO seatUpdateReq(Long areaId, int rowNo, int columnNo, String seatNo, int status) {
        SeatUpdateReqDTO request = new SeatUpdateReqDTO();
        request.setAreaId(areaId);
        request.setRowNo(rowNo);
        request.setColumnNo(columnNo);
        request.setSeatNo(seatNo);
        request.setStatus(status);
        return request;
    }

    private ShowSessionCreateReqDTO showReq(LocalDateTime showTime) {
        ShowSessionCreateReqDTO request = new ShowSessionCreateReqDTO();
        request.setShowTime(showTime);
        request.setDurationMinutes(90);
        request.setStatus(1);
        return request;
    }

    private TicketCategoryConfigReqDTO ticketCategoryReq(String name, String price, int totalStock, int seatSelectable, List<Long> seatIds) {
        TicketCategoryConfigReqDTO request = new TicketCategoryConfigReqDTO();
        request.setCategoryName(name);
        request.setPrice(new BigDecimal(price));
        request.setTotalStock(totalStock);
        request.setSeatSelectable(seatSelectable);
        request.setStatus(1);
        request.setSeatIds(seatIds);
        return request;
    }

    private PerformanceDetailRespDTO createPendingPerformance(Long venueId, String title) {
        PerformanceCreateReqDTO performanceReq = new PerformanceCreateReqDTO();
        performanceReq.setTitle(title);
        performanceReq.setPerformanceType("CONCERT");
        performanceReq.setArtistId(100L);
        performanceReq.setVenueId(venueId);
        return performanceService.create(performanceReq);
    }

    private PerformanceDetailRespDTO createPendingPerformanceWithConfiguredShow(Long venueId) {
        PerformanceCreateReqDTO performanceReq = new PerformanceCreateReqDTO();
        performanceReq.setTitle("Configured Pending Performance");
        performanceReq.setPerformanceType("CONCERT");
        performanceReq.setArtistId(100L);
        performanceReq.setVenueId(venueId);

        ShowSessionCreateReqDTO showReq = showReq(LocalDateTime.of(2026, 8, 17, 19, 30));
        TicketCategoryConfigReqDTO vipReq = ticketCategoryReq("Copy VIP", "990.00", 2, 1, List.of(400101L, 400102L));
        vipReq.setSeatMapId(400L);
        showReq.setTicketCategories(List.of(
                vipReq,
                ticketCategoryReq("Copy Standing", "199.00", 20, 0, null)
        ));
        performanceReq.setShowSessions(List.of(showReq));
        return performanceService.create(1L, performanceReq);
    }

    private ShowSessionRespDTO createEmptyShowUnderPerformance(Long performanceId, LocalDateTime showTime) {
        return performanceService.createShow(1L, performanceId, showReq(showTime));
    }

    private ShowSessionRespDTO createEmptyShow(Long venueId, LocalDateTime showTime) {
        PerformanceCreateReqDTO performanceReq = new PerformanceCreateReqDTO();
        performanceReq.setTitle("Empty Config Performance " + showTime);
        performanceReq.setPerformanceType("CONCERT");
        performanceReq.setArtistId(100L);
        performanceReq.setVenueId(venueId);
        PerformanceDetailRespDTO performance = performanceService.create(performanceReq);
        return performanceService.createShow(1L, performance.performanceId(), showReq(showTime));
    }

    private void resetService(TicketRemoteService ticketRemoteService, PerformanceSaleStartMessageSender messageSender) {
        InMemoryPerformanceRepository repository = new InMemoryPerformanceRepository();
        repository.initSeedData();
        performanceService = new PerformanceServiceImpl(
                repository,
                new IdGeneratorManager(new SnowflakeIdGenerator(1)),
                ticketRemoteService,
                messageSender
        );
    }

    private static final class CapturingTicketRemoteService implements TicketRemoteService {

        private final List<TicketShowStockInitReqDTO> copyRequests = new ArrayList<>();
        private final List<TicketShowStockConfigInitReqDTO> configRequests = new ArrayList<>();

        @Override
        public Result<Boolean> initShowStock(Long operatorId, TicketShowStockInitReqDTO requestParam) {
            copyRequests.add(requestParam);
            return Result.success(Boolean.TRUE);
        }

        @Override
        public Result<Boolean> initConfiguredShowStock(Long operatorId, TicketShowStockConfigInitReqDTO requestParam) {
            configRequests.add(requestParam);
            return Result.success(Boolean.TRUE);
        }
    }

    private static final class CapturingSaleStartMessageSender implements PerformanceSaleStartMessageSender {

        private final List<PerformanceSaleStartMessage> messages = new ArrayList<>();

        @Override
        public void send(PerformanceSaleStartMessage message) {
            messages.add(message);
        }
    }
}
