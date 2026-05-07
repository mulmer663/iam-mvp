package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.iam.registry.domain.user.IamUser;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.filters.FilterType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

/**
 * UnboundID Filter AST → JPA Specification<IamUser> 변환기.
 *
 * 지원 속성 (RFC 7643 User Schema, case-insensitive 매핑):
 *   id, externalId, userName, displayName, active,
 *   name.familyName, name.givenName, name.formatted,
 *   title, userType
 *
 * 문자열 비교: RFC 7643 §2.2 caseExact 기본값 false → LOWER() 적용.
 * active, id는 caseExact=true로 취급 (exact match).
 */
public class UserFilterSpecification {

    /** SCIM 속성명(소문자) → JPA 엔티티 필드명 */
    private static final Map<String, String> ATTR_MAP = Map.of(
            "id",              "id",
            "externalid",      "externalId",
            "username",        "userName",
            "displayname",     "displayName",
            "active",          "active",
            "name.familyname", "familyName",
            "name.givenname",  "givenName",
            "name.formatted",  "formattedName",
            "title",           "title",
            "usertype",        "userType"
    );

    /** 대소문자 exact match 속성 */
    private static final java.util.Set<String> CASE_EXACT = java.util.Set.of("id", "active");

    public Specification<IamUser> from(Filter filter) {
        return (root, query, cb) -> toPredicate(filter, root, cb);
    }

    private Predicate toPredicate(Filter filter, Root<IamUser> root, CriteriaBuilder cb) {
        FilterType type = filter.getFilterType();
        return switch (type) {
            case AND -> {
                List<Predicate> predicates = filter.getCombinedFilters().stream()
                        .map(f -> toPredicate(f, root, cb))
                        .toList();
                yield cb.and(predicates.toArray(new Predicate[0]));
            }
            case OR -> {
                List<Predicate> predicates = filter.getCombinedFilters().stream()
                        .map(f -> toPredicate(f, root, cb))
                        .toList();
                yield cb.or(predicates.toArray(new Predicate[0]));
            }
            case NOT -> cb.not(toPredicate(filter.getInvertedFilter(), root, cb));
            default -> toLeafPredicate(filter, root, cb);
        };
    }

    private Predicate toLeafPredicate(Filter filter, Root<IamUser> root, CriteriaBuilder cb) {
        String attrKey = filter.getAttributePath().toString().toLowerCase();
        String fieldName = ATTR_MAP.get(attrKey);
        if (fieldName == null) {
            throw new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                    "지원하지 않는 User 속성: " + filter.getAttributePath()
                            + ". 지원 속성: " + ATTR_MAP.keySet());
        }

        FilterType type = filter.getFilterType();

        // PR (present): IS NOT NULL
        if (type == FilterType.PRESENT) {
            return cb.isNotNull(root.get(fieldName));
        }

        // boolean 필드 특수 처리
        if ("active".equals(fieldName)) {
            boolean val = Boolean.parseBoolean(
                    filter.getComparisonValue() != null
                            ? filter.getComparisonValue().asText()
                            : "false");
            return switch (type) {
                case EQUAL     -> cb.equal(root.get(fieldName), val);
                case NOT_EQUAL -> cb.notEqual(root.get(fieldName), val);
                default        -> throw unsupportedOp(type, fieldName);
            };
        }

        // id 필드 (Long) 특수 처리
        if ("id".equals(fieldName)) {
            String rawVal = filter.getComparisonValue() != null
                    ? filter.getComparisonValue().asText() : null;
            try {
                Long longVal = Long.parseLong(rawVal);
                return switch (type) {
                    case EQUAL     -> cb.equal(root.get(fieldName), longVal);
                    case NOT_EQUAL -> cb.notEqual(root.get(fieldName), longVal);
                    default        -> throw unsupportedOp(type, fieldName);
                };
            } catch (NumberFormatException e) {
                throw new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                        "id 속성은 숫자여야 합니다: " + rawVal);
            }
        }

        // 문자열 필드
        String value = filter.getComparisonValue() != null
                ? filter.getComparisonValue().asText() : null;
        boolean isCaseExact = CASE_EXACT.contains(attrKey);

        Expression<String> fieldExpr = isCaseExact
                ? root.get(fieldName)
                : cb.lower(root.get(fieldName));
        String compareVal = (isCaseExact || value == null) ? value : value.toLowerCase();

        return switch (type) {
            case EQUAL     -> cb.equal(fieldExpr, compareVal);
            case NOT_EQUAL -> cb.notEqual(fieldExpr, compareVal);
            case CONTAINS    -> cb.like(fieldExpr, "%" + escapeLike(compareVal) + "%", '!');
            case STARTS_WITH -> cb.like(fieldExpr, escapeLike(compareVal) + "%", '!');
            case ENDS_WITH   -> cb.like(fieldExpr, "%" + escapeLike(compareVal), '!');
            default -> throw unsupportedOp(type, fieldName);
        };
    }

    private String escapeLike(String value) {
        if (value == null) return "";
        // ! % _ 순서로 이스케이프 (! 가 escape char이므로 먼저 처리)
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private IamBusinessException unsupportedOp(FilterType type, String field) {
        return new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                field + " 속성에 지원하지 않는 연산자: " + type);
    }
}
