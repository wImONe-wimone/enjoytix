package com.wimone.enjoytix.performance.dto.req;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PerformancePageQueryReqDTO {

    private String city;
    private String performanceType;
    private String artistName;
    private String venueName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showDate;

    private long current = 1;
    private long size = 20;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPerformanceType() {
        return performanceType;
    }

    public void setPerformanceType(String performanceType) {
        this.performanceType = performanceType;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current <= 0 ? 1 : current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size <= 0 ? 20 : Math.min(size, 200);
    }
}
