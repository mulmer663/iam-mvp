package com.iam.core.application.dto;

import com.iam.core.domain.vo.UniversalData;
import java.util.List;
import java.util.Map;

/**
 * Result of a transformation process including the data and the trace of rules
 * applied.
 *
 * @param data                  The transformed data map
 * @param appliedRuleVersionIds List of Rule Version IDs that were applied
 */
public record TransformationResult(
        Map<String, UniversalData> data,
        Long revId) {
}
