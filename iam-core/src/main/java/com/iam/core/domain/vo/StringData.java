package com.iam.core.domain.vo;

public record StringData(String value) implements UniversalData {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String asString() {
        return value;
    }
}
