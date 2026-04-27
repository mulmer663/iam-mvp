package com.iam.registry.application.scim;

import com.iam.registry.domain.common.ExtensionData;
import com.iam.registry.domain.common.vo.ListData;
import com.iam.registry.domain.common.vo.MapData;
import com.iam.registry.domain.common.vo.NullData;
import com.iam.registry.domain.common.vo.ScimAddress;
import com.iam.registry.domain.common.vo.ScimMultiValue;
import com.iam.registry.domain.common.vo.UniversalData;
import com.iam.registry.domain.scim.IamAttributeMeta;
import com.iam.registry.domain.user.IamUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflectively maps {@link UniversalData} values onto {@link IamUser} core
 * fields (driven by {@link IamAttributeMeta#getName()}) and onto extension
 * containers (always {@link ExtensionData} after the extension generalization).
 */
@Component
@Slf4j
public class EntityAttributeBinder {

    private final Map<String, Field> userFieldCache = new ConcurrentHashMap<>();

    public void bindCoreAttribute(IamUser user, IamAttributeMeta meta, UniversalData data) {
        String fieldName = meta.getName();
        Field field = userFieldCache.computeIfAbsent(fieldName,
                k -> ReflectionUtils.findField(IamUser.class, fieldName));

        if (field == null) {
            log.warn("Core field {} not found in IamUser entity", fieldName);
            return;
        }

        ReflectionUtils.makeAccessible(field);
        Object value = convertData(field.getType(), data);
        ReflectionUtils.setField(field, user, value);
    }

    public void bindExtensionAttribute(ExtensionData extension, IamAttributeMeta meta, UniversalData data) {
        // Post-generalization: ExtensionData is the only container — no typed
        // mirror class with structured fields, so all extension values land in
        // the @JsonAnyGetter-backed attributes map.
        extension.add(meta.getName(), data.getValue());
    }

    private Object convertData(Class<?> type, UniversalData data) {
        if (data instanceof NullData) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == double.class) return 0.0;
            return null;
        }

        if (type == String.class) return data.asString();
        if (type == boolean.class || type == Boolean.class) {
            if (data.getValue() instanceof Boolean b) return b;
            return Boolean.parseBoolean(data.asString());
        }
        if (type == int.class || type == Integer.class) return Integer.parseInt(data.asString());
        if (type == long.class || type == Long.class) return Long.parseLong(data.asString());

        if (type == List.class) {
            if (data instanceof ListData listData) {
                return convertList(listData);
            }
            return null;
        }

        if (type == java.util.Set.class) {
            if (data instanceof ListData listData) {
                return new java.util.HashSet<>(convertList(listData));
            }
            return null;
        }

        return data.getValue();
    }

    private List<?> convertList(ListData listData) {
        if (listData.getValue() instanceof List<?> list && !list.isEmpty()) {
            UniversalData first = (UniversalData) list.getFirst();
            if (first instanceof MapData) {
                return list.stream()
                        .map(item -> convertMap((MapData) item))
                        .toList();
            }
        }
        return java.util.Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Object convertMap(MapData mapData) {
        Map<String, UniversalData> map = (Map<String, UniversalData>) mapData.getValue();

        if (map.containsKey("country") || map.containsKey("region") || map.containsKey("postalCode")) {
            return ScimAddress.builder()
                    .country(getAsString(map, "country"))
                    .region(getAsString(map, "region"))
                    .locality(getAsString(map, "locality"))
                    .postalCode(getAsString(map, "postalCode"))
                    .streetAddress(getAsString(map, "streetAddress"))
                    .formatted(getAsString(map, "formatted"))
                    .type(getAsString(map, "type"))
                    .primary(getAsBoolean(map, "primary"))
                    .build();
        }

        return ScimMultiValue.builder()
                .value(getAsString(map, "value"))
                .display(getAsString(map, "display"))
                .type(getAsString(map, "type"))
                .primary(getAsBoolean(map, "primary"))
                .ref(getAsString(map, "$ref"))
                .build();
    }

    private String getAsString(Map<String, UniversalData> map, String key) {
        UniversalData data = map.get(key);
        return data != null ? data.asString() : null;
    }

    private boolean getAsBoolean(Map<String, UniversalData> map, String key) {
        UniversalData data = map.get(key);
        return data != null && data.asBoolean();
    }
}
