package com.ailene.lms.common.pagination;

public final class Pagination {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static long offset(int page, int pageSize) {
        return (long) (page - 1) * pageSize;
    }

    public static PageMeta meta(long total, int page, int pageSize) {
        int totalPage = total > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
        return new PageMeta(total, totalPage, page, pageSize);
    }
}
