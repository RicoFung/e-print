package com.eprint.admin.common.support;

public final class PageSupport {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MIN_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private PageSupport() {
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize < MIN_PAGE_SIZE) {
            return MIN_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
