package com.wimone.enjoytix.performance.service;

import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;

import java.util.List;

public interface PerformanceService {

    PageResponse<PerformanceListRespDTO> pageQuery(PerformancePageQueryReqDTO requestParam);

    PerformanceDetailRespDTO detail(Long performanceId);

    ShowSessionRespDTO show(Long showId);

    List<TicketCategoryRespDTO> ticketCategories(Long showId);

    SeatMapRespDTO seatMap(Long showId);
}
