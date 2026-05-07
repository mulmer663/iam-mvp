package com.iam.registry.application.scim.filter;

import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.IamBusinessException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.filters.FilterType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UnboundID Filter AST → native PostgreSQL WHERE 절 빌더.
 *
 * scim_dynamic_resource 테이블 대상:
 *   - id        → scim_id 컬럼 (text)
 *   - externalId → external_id 컬럼 (text)
 *   - displayName, active, deptCode → attributes JSONB (->>)
 *
 * 값은 항상 named parameter로 바인딩하여 SQL Injection 방지.
 */
public class DynamicResourceFilterQuery {

    /** SCIM 속성명(소문자) → SQL 표현식 템플릿 (? 자리는 파라미터명으로 교체) */
    private static final Map<String, ColumnDef> COLUMN_MAP = Map.of(
            "id",          new ColumnDef("scim_id",                           false),
            "externalid",  new ColumnDef("external_id",                       false),
            "displayname", new ColumnDef("attributes->>'displayName'",        true),
            "active",      new ColumnDef("attributes->>'active'",             true),
            "deptcode",    new ColumnDef("attributes->>'deptCode'",           true),
            "level",       new ColumnDef("attributes->>'level'",              true),
            "parentid",    new ColumnDef("attributes->>'parentId'",           true)
    );

    /** caseExact=true로 취급하는 속성 */
    private static final Set<String> CASE_EXACT = Set.of("id", "active", "deptcode", "level", "parentid");

    private final Map<String, Object> params = new HashMap<>();
    private int paramIndex = 0;

    @Getter
    private String whereClause;

    /**
     * @return WHERE 절 문자열 (resourceType 조건 제외, 호출측에서 AND 연결)
     */
    public Map<String, Object> build(Filter filter) {
        params.clear();
        paramIndex = 0;
        whereClause = buildClause(filter);
        return Map.copyOf(params);
    }

    private String buildClause(Filter filter) {
        FilterType type = filter.getFilterType();
        return switch (type) {
            case AND -> {
                List<String> parts = filter.getCombinedFilters().stream()
                        .map(this::buildClause).toList();
                yield "(" + String.join(" AND ", parts) + ")";
            }
            case OR -> {
                List<String> parts = filter.getCombinedFilters().stream()
                        .map(this::buildClause).toList();
                yield "(" + String.join(" OR ", parts) + ")";
            }
            case NOT -> "NOT (" + buildClause(filter.getInvertedFilter()) + ")";
            default -> buildLeafClause(filter);
        };
    }

    private String buildLeafClause(Filter filter) {
        String attrKey = filter.getAttributePath().toString().toLowerCase();
        ColumnDef col = COLUMN_MAP.get(attrKey);
        if (col == null) {
            throw new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                    "지원하지 않는 동적 리소스 속성: " + filter.getAttributePath()
                            + ". 지원 속성: " + COLUMN_MAP.keySet());
        }

        FilterType type = filter.getFilterType();
        String sqlExpr = col.sqlExpression();
        boolean caseExact = CASE_EXACT.contains(attrKey);

        if (type == FilterType.PRESENT) {
            return sqlExpr + " IS NOT NULL";
        }

        String rawValue = filter.getComparisonValue() != null
                ? filter.getComparisonValue().asText() : null;
        String paramName = "p" + (paramIndex++);

        if (!caseExact && rawValue != null) {
            // case-insensitive: LOWER(expr) = LOWER(:param)
            String lowerParam = "p" + (paramIndex++);
            String lowerExpr = "LOWER(" + sqlExpr + ")";
            String lowerRef = ":" + lowerParam;

            return switch (type) {
                case EQUAL     -> { params.put(lowerParam, rawValue.toLowerCase());
                                    yield lowerExpr + " = " + lowerRef; }
                case NOT_EQUAL -> { params.put(lowerParam, rawValue.toLowerCase());
                                    yield lowerExpr + " != " + lowerRef; }
                case CONTAINS  -> { params.put(lowerParam, escapeLike(rawValue.toLowerCase()));
                                    yield lowerExpr + " LIKE '%' || " + lowerRef + " || '%' ESCAPE '!'"; }
                case STARTS_WITH -> { params.put(lowerParam, escapeLike(rawValue.toLowerCase()));
                                      yield lowerExpr + " LIKE " + lowerRef + " || '%' ESCAPE '!'"; }
                case ENDS_WITH   -> { params.put(lowerParam, escapeLike(rawValue.toLowerCase()));
                                      yield lowerExpr + " LIKE '%' || " + lowerRef + " ESCAPE '!'"; }
                default -> throw unsupportedOp(type, attrKey);
            };
        }

        params.put(paramName, rawValue);
        String paramRef = ":" + paramName;

        return switch (type) {
            case EQUAL     -> sqlExpr + " = " + paramRef;
            case NOT_EQUAL -> sqlExpr + " != " + paramRef;
            case CONTAINS    -> { params.put(paramName, escapeLike(rawValue));
                                  yield sqlExpr + " LIKE '%' || " + paramRef + " || '%' ESCAPE '!'"; }
            case STARTS_WITH -> { params.put(paramName, escapeLike(rawValue));
                                  yield sqlExpr + " LIKE " + paramRef + " || '%' ESCAPE '!'"; }
            case ENDS_WITH   -> { params.put(paramName, escapeLike(rawValue));
                                  yield sqlExpr + " LIKE '%' || " + paramRef + " ESCAPE '!'"; }
            default -> throw unsupportedOp(type, attrKey);
        };
    }

    private String escapeLike(String value) {
        if (value == null) return "";
        // ! 가 escape char이므로 먼저 처리
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private IamBusinessException unsupportedOp(FilterType type, String attr) {
        return new IamBusinessException(ErrorCode.INVALID_SCIM_FILTER, "FILTER",
                attr + " 속성에 지원하지 않는 연산자: " + type);
    }

    private record ColumnDef(String sqlExpression, boolean jsonbPath) {}
}
