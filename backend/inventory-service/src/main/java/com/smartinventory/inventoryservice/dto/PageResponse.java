package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Standard uniform pagination response DTO conforming to Section 2.3 of API_SPECIFICATION.md.
 *
 * @param <T> Type of the items in content list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean first;

    private boolean last;

    /**
     * Converts a Spring Data Page to PageResponse using a mapper function.
     *
     * @param page      Spring Data Page
     * @param converter Entity to DTO conversion function
     * @param <E>       Source entity type
     * @param <T>       Target DTO type
     * @return Standardized PageResponse
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> converter) {
        List<T> content = page.getContent().stream()
                .map(converter)
                .collect(Collectors.toList());

        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
