package com.wimone.enjoytix.framework.convention.page;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class PageResponse<T> implements Serializable {

    private long current;
    private long size;
    private long total;
    private List<T> records = Collections.emptyList();

    public PageResponse() {
    }

    public PageResponse(long current, long size, long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
