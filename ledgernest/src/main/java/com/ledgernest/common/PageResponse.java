package com.ledgernest.common;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// Generic wrapper for any paginated list response
// Usage: PageResponse<InvoiceResponse>, PageResponse<ClientResponse>, etc.
@Getter
@Builder

public class PageResponse<T> {

    private List<T> content;       // The actual items on this page
    private int page;              // Current page number (0-based)
    private int size;              // Items per page
    private long totalElements;    // Total items across ALL pages
    private int totalPages;        // Total number of pages
    private boolean last;          // Is this the last page?

    // Spring gives you a Page<T> object from the repository
    // This converts it into your clean PageResponse format
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}