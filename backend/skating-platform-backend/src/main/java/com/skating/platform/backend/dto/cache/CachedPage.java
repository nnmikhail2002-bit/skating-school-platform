
package com.skating.platform.backend.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CachedPage {

    private List<Object> content = new ArrayList<>();
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private List<CachedSortOrder> sortOrders = new ArrayList<>();

    public static CachedPage from(Page<?> page) {

        List<Object> content = new ArrayList<>(page.getContent());

        List<CachedSortOrder> sortOrders = page.getSort()
                .stream()
                .map(CachedSortOrder::from)
                .toList();

        return new CachedPage(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                sortOrders
        );
    }

    public Page<?> toPage() {

        Sort sort = sortOrders.isEmpty()
                ? Sort.unsorted()
                : Sort.by(
                sortOrders.stream()
                        .map(CachedSortOrder::toOrder)
                        .toList()
        );

        return new PageImpl<>(
                content,
                PageRequest.of(pageNumber, pageSize, sort),
                totalElements
        );
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedSortOrder {

        private String property;
        private String direction;
        private boolean ignoreCase;
        private String nullHandling;

        public static CachedSortOrder from(Sort.Order order) {

            return new CachedSortOrder(
                    order.getProperty(),
                    order.getDirection().name(),
                    order.isIgnoreCase(),
                    order.getNullHandling().name()
            );
        }

        public Sort.Order toOrder() {

            Sort.Order order = new Sort.Order(
                    Sort.Direction.valueOf(direction),
                    property
            );

            if (ignoreCase) {
                order = order.ignoreCase();
            }

            return order.with(
                    Sort.NullHandling.valueOf(nullHandling)
            );
        }
    }
}

