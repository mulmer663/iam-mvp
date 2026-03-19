package com.iam.registry.domain.common.vo;

import java.util.Collections;
import java.util.Map;

public record MapData(Map<String, UniversalData> map) implements UniversalData {
    public MapData {
        if (map == null)
            map = Collections.emptyMap();
    }

    @Override
    public Object getValue() {
        return map;
    }

    @Override
    public String asString() {
        return map.toString();
    }
}
