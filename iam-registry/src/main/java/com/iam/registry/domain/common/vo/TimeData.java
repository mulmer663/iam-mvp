package com.iam.registry.domain.common.vo;

import java.time.LocalDateTime;

public record TimeData(LocalDateTime value) implements UniversalData {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String asString() {
        return value != null ? value.toString() : "";
    }
}
