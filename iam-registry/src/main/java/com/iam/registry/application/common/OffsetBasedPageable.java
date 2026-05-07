package com.iam.registry.application.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * RFC 7644 §3.4.2.4: startIndex는 1-based 임의 offset.
 * Spring Data PageRequest는 페이지 번호 기반이라 비정렬 offset 표현 불가.
 * 이 구현은 getOffset()을 실제 OFFSET 값으로 반환하여 JPA setFirstResult에 전달.
 */
public record OffsetBasedPageable(long offset, int limit, Sort sort) implements Pageable {

    public static OffsetBasedPageable of(int offset, int limit) {
        return new OffsetBasedPageable(offset, limit, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public int getPageNumber() {
        return limit == 0 ? 0 : (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageable(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return offset == 0 ? this : new OffsetBasedPageable(Math.max(0, offset - limit), limit, sort);
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageable(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetBasedPageable((long) pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
