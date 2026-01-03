package com.iam.core.domain.vo;

public record BooleanData(Boolean value) implements UniversalData {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public Boolean asBoolean() {
        return value != null && value;
    }

    @Override
    public Integer asInt() {
        return (value != null && value) ? 1 : 0;
    }
}
