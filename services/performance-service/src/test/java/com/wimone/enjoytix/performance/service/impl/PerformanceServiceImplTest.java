package com.wimone.enjoytix.performance.service.impl;

import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;
import com.wimone.enjoytix.performance.repository.InMemoryPerformanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
        performanceService = new PerformanceServiceImpl(repository);
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
}
