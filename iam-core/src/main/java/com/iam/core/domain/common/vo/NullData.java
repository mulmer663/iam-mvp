package com.iam.core.domain.common.vo;

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

    @Override
    public Integer asInt() {
        return 0;
    }

    @Override
    public Long asLong() {
        return 0L;
    }

    @Override
    public Boolean asBoolean() {
        return false;
    }
}
