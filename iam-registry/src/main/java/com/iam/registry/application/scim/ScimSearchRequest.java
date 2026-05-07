package com.iam.registry.application.scim;

/**
 * RFC 7644 §3.4.2 — ListResponse 요청 파라미터.
 * startIndex는 1-based. count=0이면 totalResults만 반환.
 */
public record ScimSearchRequest(String filter, int startIndex, int count) {

    private static final int MAX_COUNT = 200;
    private static final int DEFAULT_COUNT = 100;

    public static ScimSearchRequest of(String filter, Integer startIndex, Integer count) {
        int si = (startIndex == null || startIndex < 1) ? 1 : startIndex;
        int c;
        if (count == null) {
            c = DEFAULT_COUNT;
        } else if (count < 0) {
            c = DEFAULT_COUNT;
        } else {
            c = Math.min(count, MAX_COUNT);
        }
        return new ScimSearchRequest(filter, si, c);
    }

    /** RFC 7644: count=0이면 리소스 없이 totalResults만 반환 */
    public boolean isCountOnly() {
        return count == 0;
    }

    /** JPA offset 계산 (0-based) */
    public int offset() {
        return startIndex - 1;
    }
}
