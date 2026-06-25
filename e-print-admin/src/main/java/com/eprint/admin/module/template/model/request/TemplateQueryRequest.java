package com.eprint.admin.module.template.model.request;

public class TemplateQueryRequest {

    private String templateTypeId;
    private String templateCode;
    private Integer status;
    private String search;
    private String sort;
    private Integer offset = 0;
    private Integer limit = 10;
    private Integer page = 1;
    private Integer pageSize = 10;

    public int page() {
        int size = limit();
        return offset == null || offset < 0 ? 1 : (offset / size) + 1;
    }

    public int limit() {
        return limit == null || limit < 1 ? 10 : limit;
    }

    public int initialPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int initialPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    public String queryTemplateCode() {
        return templateCode == null || templateCode.isBlank() ? search : templateCode;
    }

    public String getTemplateTypeId() {
        return templateTypeId;
    }

    public void setTemplateTypeId(String templateTypeId) {
        this.templateTypeId = templateTypeId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
