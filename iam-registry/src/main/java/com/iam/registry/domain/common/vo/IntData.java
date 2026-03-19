package com.iam.registry.domain.common.vo;

public record IntData(Integer value) implements UniversalData {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public Integer asInt() {
        return value != null ? value : 0;
    }

    @Override
    public Long asLong() {
        return value != null ? value.longValue() : 0L;
    }

    @Override
    public Double asDouble() {
        return value != null ? value.doubleValue() : 0.0;
    }
}
