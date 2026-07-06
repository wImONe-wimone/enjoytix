package com.wimone.enjoytix.performance.repository;

import com.wimone.enjoytix.performance.common.enums.PerformanceTypeEnum;
import com.wimone.enjoytix.performance.dao.entity.ArtistDO;
import com.wimone.enjoytix.performance.dao.entity.HallDO;
import com.wimone.enjoytix.performance.dao.entity.PerformanceDO;
import com.wimone.enjoytix.performance.dao.entity.SeatDO;
import com.wimone.enjoytix.performance.dao.entity.SeatMapDO;
import com.wimone.enjoytix.performance.dao.entity.ShowSessionDO;
import com.wimone.enjoytix.performance.dao.entity.TicketCategoryDO;
import com.wimone.enjoytix.performance.dao.entity.VenueDO;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryPerformanceRepository implements PerformanceRepository {

    private final Map<Long, ArtistDO> artists = new ConcurrentHashMap<>();
    private final Map<Long, VenueDO> venues = new ConcurrentHashMap<>();
    private final Map<Long, HallDO> halls = new ConcurrentHashMap<>();
    private final Map<Long, PerformanceDO> performances = new ConcurrentHashMap<>();
    private final Map<Long, ShowSessionDO> sessions = new ConcurrentHashMap<>();
    private final Map<Long, TicketCategoryDO> categories = new ConcurrentHashMap<>();
    private final Map<Long, SeatMapDO> seatMaps = new ConcurrentHashMap<>();
    private final Map<Long, SeatDO> seats = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSeedData() {
        ArtistDO artist = artist(100L, "Aurora Band", "Electronic pop live show");
        VenueDO venue = venue(
                200L,
                "Enjoy Arena",
                "中国",
                "北京市",
                "北京市",
                "朝阳区",
                null,
                null,
                "阜通东大街",
                "6号",
                null,
                null,
                "北京市朝阳区阜通东大街6号"
        );
        SeatMapDO seatMap = seatMap(400L, "Enjoy Arena Main Hall", 6, 8);
        HallDO hall = hall(300L, venue.getId(), "Main Hall", seatMap.getId());
        PerformanceDO performance = performance(
                1001L,
                "Aurora Band 2026 Live",
                PerformanceTypeEnum.CONCERT.name(),
                artist.getId(),
                venue.getId(),
                venue.getCity(),
                "https://static.enjoytix.local/posters/aurora-live.jpg",
                "A high demand concert used by EnjoyTix MVP flash-sale scenarios"
        );
        ShowSessionDO show = showSession(
                2001L,
                performance.getId(),
                hall.getId(),
                LocalDateTime.of(2026, 8, 16, 19, 30),
                LocalDateTime.of(2026, 7, 20, 12, 0),
                LocalDateTime.of(2026, 8, 16, 19, 0)
        );
        ticketCategory(3001L, show.getId(), "VIP", new BigDecimal("1280.00"), 12, 1);
        ticketCategory(3002L, show.getId(), "A Zone", new BigDecimal("880.00"), 18, 1);
        ticketCategory(3003L, show.getId(), "B Zone", new BigDecimal("580.00"), 18, 1);
        seedSeats(seatMap.getId());

        ArtistDO dramaArtist = artist(101L, "North Theatre", "Modern drama troupe");
        VenueDO theatre = venue(
                201L,
                "River Theatre",
                "中国",
                "北京市",
                "北京市",
                "东城区",
                null,
                null,
                "东长安街",
                "16号",
                null,
                null,
                "北京市东城区东长安街16号"
        );
        SeatMapDO theatreSeatMap = seatMap(401L, "River Theatre Hall A", 4, 6);
        HallDO theatreHall = hall(301L, theatre.getId(), "Hall A", theatreSeatMap.getId());
        PerformanceDO drama = performance(
                1002L,
                "Night Train Drama",
                PerformanceTypeEnum.DRAMA.name(),
                dramaArtist.getId(),
                theatre.getId(),
                theatre.getCity(),
                "https://static.enjoytix.local/posters/night-train.jpg",
                "Small theatre drama with seat selection"
        );
        ShowSessionDO dramaShow = showSession(
                2002L,
                drama.getId(),
                theatreHall.getId(),
                LocalDateTime.of(2026, 9, 3, 20, 0),
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 9, 3, 19, 30)
        );
        ticketCategory(3004L, dramaShow.getId(), "Standard", new BigDecimal("280.00"), 24, 1);
        seedSeats(theatreSeatMap.getId());
    }

    @Override
    public List<PerformanceDO> listPerformances() {
        return performances.values().stream().sorted(Comparator.comparing(PerformanceDO::getId)).toList();
    }

    @Override
    public Optional<PerformanceDO> findPerformance(Long performanceId) {
        return Optional.ofNullable(performances.get(performanceId));
    }

    @Override
    public Optional<ArtistDO> findArtist(Long artistId) {
        return Optional.ofNullable(artists.get(artistId));
    }

    @Override
    public List<ArtistDO> listArtistsByIds(List<Long> artistIds) {
        if (artistIds == null || artistIds.isEmpty()) {
            return List.of();
        }
        return artistIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(artists::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<VenueDO> findVenue(Long venueId) {
        return Optional.ofNullable(venues.get(venueId));
    }

    @Override
    public List<VenueDO> listVenuesByIds(List<Long> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return List.of();
        }
        return venueIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(venues::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<HallDO> findHall(Long hallId) {
        return Optional.ofNullable(halls.get(hallId));
    }

    @Override
    public Optional<ShowSessionDO> findShow(Long showId) {
        return Optional.ofNullable(sessions.get(showId));
    }

    @Override
    public List<ShowSessionDO> listShowsByPerformance(Long performanceId) {
        return sessions.values()
                .stream()
                .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                .sorted(Comparator.comparing(ShowSessionDO::getShowTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    @Override
    public List<ShowSessionDO> listShowsByPerformanceIds(List<Long> performanceIds) {
        if (performanceIds == null || performanceIds.isEmpty()) {
            return List.of();
        }
        return sessions.values()
                .stream()
                .filter(each -> performanceIds.contains(each.getPerformanceId()))
                .sorted(Comparator.comparing(ShowSessionDO::getPerformanceId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(ShowSessionDO::getShowTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    @Override
    public List<TicketCategoryDO> listCategoriesByShow(Long showId) {
        return categories.values()
                .stream()
                .filter(each -> Objects.equals(showId, each.getShowId()))
                .sorted(Comparator.comparing(TicketCategoryDO::getPrice, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public Optional<SeatMapDO> findSeatMap(Long seatMapId) {
        return Optional.ofNullable(seatMaps.get(seatMapId));
    }

    @Override
    public List<SeatDO> listSeatsBySeatMap(Long seatMapId) {
        return seats.values()
                .stream()
                .filter(each -> Objects.equals(seatMapId, each.getSeatMapId()))
                .sorted(Comparator.comparing(SeatDO::getRowNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SeatDO::getColumnNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private ArtistDO artist(Long id, String name, String description) {
        ArtistDO entity = new ArtistDO();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription(description);
        entity.setDelFlag(0);
        artists.put(id, entity);
        return entity;
    }

    private VenueDO venue(
            Long id,
            String name,
            String country,
            String province,
            String city,
            String district,
            String town,
            String village,
            String street,
            String houseNumber,
            String estate,
            String building,
            String address) {
        VenueDO entity = new VenueDO();
        entity.setId(id);
        entity.setName(name);
        entity.setCountry(country);
        entity.setProvince(province);
        entity.setCity(city);
        entity.setDistrict(district);
        entity.setTown(town);
        entity.setVillage(village);
        entity.setStreet(street);
        entity.setHouseNumber(houseNumber);
        entity.setEstate(estate);
        entity.setBuilding(building);
        entity.setAddress(address);
        entity.setDelFlag(0);
        venues.put(id, entity);
        return entity;
    }

    private HallDO hall(Long id, Long venueId, String name, Long seatMapId) {
        HallDO entity = new HallDO();
        entity.setId(id);
        entity.setVenueId(venueId);
        entity.setName(name);
        entity.setSeatMapId(seatMapId);
        entity.setDelFlag(0);
        halls.put(id, entity);
        return entity;
    }

    private SeatMapDO seatMap(Long id, String name, Integer rowCount, Integer columnCount) {
        SeatMapDO entity = new SeatMapDO();
        entity.setId(id);
        entity.setName(name);
        entity.setRowCount(rowCount);
        entity.setColumnCount(columnCount);
        entity.setDelFlag(0);
        seatMaps.put(id, entity);
        return entity;
    }

    private PerformanceDO performance(Long id, String title, String type, Long artistId, Long venueId, String city, String posterUrl, String description) {
        PerformanceDO entity = new PerformanceDO();
        entity.setId(id);
        entity.setTitle(title);
        entity.setPerformanceType(type);
        entity.setArtistId(artistId);
        entity.setVenueId(venueId);
        entity.setCity(city);
        entity.setPosterUrl(posterUrl);
        entity.setDescription(description);
        entity.setStatus(1);
        entity.setDelFlag(0);
        performances.put(id, entity);
        return entity;
    }

    private ShowSessionDO showSession(Long id, Long performanceId, Long hallId, LocalDateTime showTime, LocalDateTime saleStart, LocalDateTime saleEnd) {
        ShowSessionDO entity = new ShowSessionDO();
        entity.setId(id);
        entity.setPerformanceId(performanceId);
        entity.setHallId(hallId);
        entity.setShowTime(showTime);
        entity.setSaleStartTime(saleStart);
        entity.setSaleEndTime(saleEnd);
        entity.setStatus(1);
        entity.setDelFlag(0);
        sessions.put(id, entity);
        return entity;
    }

    private void ticketCategory(Long id, Long showId, String name, BigDecimal price, Integer stock, Integer seatSelectable) {
        TicketCategoryDO entity = new TicketCategoryDO();
        entity.setId(id);
        entity.setShowId(showId);
        entity.setCategoryName(name);
        entity.setPrice(price);
        entity.setTotalStock(stock);
        entity.setRemainingStock(stock);
        entity.setSeatSelectable(seatSelectable);
        entity.setStatus(1);
        entity.setDelFlag(0);
        categories.put(id, entity);
    }

    private void seedSeats(Long seatMapId) {
        SeatMapDO seatMap = seatMaps.get(seatMapId);
        List<SeatDO> generatedSeats = new ArrayList<>();
        long base = seatMapId * 1000;
        for (int row = 1; row <= seatMap.getRowCount(); row++) {
            for (int column = 1; column <= seatMap.getColumnCount(); column++) {
                SeatDO seat = new SeatDO();
                seat.setId(base + row * 100L + column);
                seat.setSeatMapId(seatMapId);
                seat.setAreaName(row <= 2 ? "Front" : "Standard");
                seat.setRowNo(row);
                seat.setColumnNo(column);
                seat.setSeatNo((char) ('A' + row - 1) + String.valueOf(column));
                seat.setStatus(1);
                seat.setDelFlag(0);
                generatedSeats.add(seat);
            }
        }
        generatedSeats.forEach(each -> seats.put(each.getId(), each));
    }
}
