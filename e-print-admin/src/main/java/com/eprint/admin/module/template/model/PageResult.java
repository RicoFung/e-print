package com.eprint.admin.module.template.model;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private final List<T> records;
    private final int page;
    private final int pageSize;
    private final int total;
    private final int totalPages;

    public PageResult(List<T> records, int page, int pageSize, int total) {
        this.records = records == null ? Collections.emptyList() : records;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return totalPages > 0 && page < totalPages;
    }

    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return totalPages == 0 ? 1 : Math.min(totalPages, page + 1);
    }

    public int getStartRow() {
        return total == 0 ? 0 : ((page - 1) * pageSize) + 1;
    }

    public int getEndRow() {
        return Math.min(page * pageSize, total);
    }
}
