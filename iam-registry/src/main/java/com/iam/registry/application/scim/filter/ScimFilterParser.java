package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.filters.FilterType;

import java.util.Set;

/**
 * UnboundID SCIM2 SDK로 filter 문자열을 파싱한 뒤,
 * Phase A 화이트리스트 연산자만 허용.
 *
 * 지원: eq ne pr co sw ew and or not
 * 미지원(Phase B): gt ge lt le, 복합값 path (emails[type eq "work"])
 */
public class ScimFilterParser {

    private static final int MAX_FILTER_LENGTH = 2048;
    private static final Set<FilterType> SUPPORTED = Set.of(
            FilterType.EQUAL,
            FilterType.NOT_EQUAL,
            FilterType.PRESENT,
            FilterType.CONTAINS,
            FilterType.STARTS_WITH,
            FilterType.ENDS_WITH,
            FilterType.AND,
            FilterType.OR,
            FilterType.NOT
    );

    public Filter parse(String filterString) {
        if (filterString == null || filterString.isBlank()) {
            return null;
        }
        if (filterString.length() > MAX_FILTER_LENGTH) {
            throw invalidFilter("filter 표현식 길이 초과 (max=" + MAX_FILTER_LENGTH + ")");
        }
        try {
            Filter filter = Filter.fromString(filterString);
            validateOperators(filter);
            return filter;
        } catch (BadRequestException e) {
            throw invalidFilter("파싱 오류: " + e.getMessage());
        }
    }

    private void validateOperators(Filter filter) {
        FilterType type = filter.getFilterType();
        if (!SUPPORTED.contains(type)) {
            throw invalidFilter("지원하지 않는 연산자: " + type
                    + " (Phase A 지원: eq ne pr co sw ew and or not)");
        }
        switch (type) {
            case AND, OR -> filter.getCombinedFilters().forEach(this::validateOperators);
            case NOT -> validateOperators(filter.getInvertedFilter());
            default -> { /* leaf filter — no recursion needed */ }
        }
    }

    private IamBusinessException invalidFilter(String detail) {
        return new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                "invalidFilter: " + detail);
    }
}
