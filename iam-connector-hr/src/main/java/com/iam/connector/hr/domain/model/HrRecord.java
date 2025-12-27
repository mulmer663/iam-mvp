package com.iam.connector.hr.domain.model;

import java.util.Map;

/**
 * Domain model representing a single record from the HR system.
 */
public record HrRecord(
        String externalId,
        Map<String, Object> data,
        String hash) {
}
