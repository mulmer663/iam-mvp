package com.iam.core.domain.vo;

import java.util.Collections;
import java.util.List;

public record ListData(List<UniversalData> list) implements UniversalData {
    public ListData {
        if (list == null)
            list = Collections.emptyList();
    }

    @Override
    public Object getValue() {
        return list;
    }

    @Override
    public String asString() {
        return list.toString();
    }
}
