package com.iam.core.domain.vo;

public record IntData(Integer value) implements UniversalData {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }
}
