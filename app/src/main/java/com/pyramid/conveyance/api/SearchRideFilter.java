package com.pyramid.conveyance.api;

public class SearchRideFilter {
    int page;
    int rows;
    String searchString;
    String sortBy;
    String sortOrder;
    String branch;
    String role;
    String conveyanceApplicable;
    String dateFrom;
    String dateTo;
    String onLeave;

    public SearchRideFilter(int page, int rows, String searchString, String sortBy, String sortOrder, String branch, String role, String conveyanceApplicable, String dateFrom, String dateTo, String onLeave) {
        this.page = page;
        this.rows = rows;
        this.searchString = searchString;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.branch = branch;
        this.role = role;
        this.conveyanceApplicable = conveyanceApplicable;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.onLeave = onLeave;
    }

    public SearchRideFilter(String dateFrom, String dateTo) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getConveyanceApplicable() {
        return conveyanceApplicable;
    }

    public void setConveyanceApplicable(String conveyanceApplicable) {
        this.conveyanceApplicable = conveyanceApplicable;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getOnLeave() {
        return onLeave;
    }

    public void setOnLeave(String onLeave) {
        this.onLeave = onLeave;
    }
}
