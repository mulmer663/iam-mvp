package com.iam.core.domain.vo;

import lombok.Value;

/**
 * Represents a null or missing value in UniversalData.
 */
@Value
public class NullData implements UniversalData {
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String asString() {
        return "";
    }
}
