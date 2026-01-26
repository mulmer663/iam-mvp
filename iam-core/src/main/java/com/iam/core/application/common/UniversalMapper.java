package com.iam.core.application.common;

import com.iam.core.domain.common.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts raw Map data (from JSON) to Map<String, UniversalData> for the rule
 * engine.
 */
@Component
@Slf4j
public class UniversalMapper {

    public Map<String, UniversalData> map(Map<String, Object> raw) {
        Map<String, UniversalData> result = new HashMap<>();
        if (raw == null)
            return result;

        raw.forEach((k, v) -> {
            UniversalData data = toUniversalData(v);
            if (data != null) {
                result.put(k, data);
            }
        });
        return result;
    }

    public UniversalData toUniversalData(Object v) {
        if (v == null)
            return new NullData();

        return switch (v) {
            case String s -> new StringData(s);
            case Integer i -> new IntData(i);
            case Long l -> new IntData(l.intValue());
            case Boolean b -> new BooleanData(b);
            case LocalDateTime t -> new TimeData(t);
            case Map<?, ?> m -> new StringData(m.toString());
            default -> new StringData(v.toString());
        };
    }
}
