package com.civicdesk.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Lightweight, framework-neutral pagination envelope matching the API contract. */
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    public PageResponse() {
    }

    public PageResponse(List<T> content, long totalElements, int totalPages, int currentPage) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    /** Build from a Spring Data {@link Page}, mapping each element with {@code mapper}. */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber());
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
