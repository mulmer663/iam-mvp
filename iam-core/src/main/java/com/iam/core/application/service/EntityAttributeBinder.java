package com.iam.core.application.service;

import com.iam.core.domain.entity.ExtensionData;
import com.iam.core.domain.entity.IamAttributeMeta;
import com.iam.core.domain.entity.IamUser;
import com.iam.core.domain.vo.NullData;
import com.iam.core.domain.vo.UniversalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        String fieldName = meta.getName();
        Field field = ReflectionUtils.findField(extension.getClass(), fieldName);

        if (field != null) {
            // 정형화된 필드에 바인딩 (e.g. EnterpriseUserExtension.employeeNumber)
            ReflectionUtils.makeAccessible(field);
            Object value = convertData(field.getType(), data);
            ReflectionUtils.setField(field, extension, value);
        } else {
            // 정형화되지 않은 필드는 Generic Map에 저장
            extension.add(fieldName, data.getValue());
        }
    }

    private Object convertData(Class<?> type, UniversalData data) {
        if (data instanceof NullData) {
            if (type == boolean.class)
                return false;
            if (type == int.class)
                return 0;
            if (type == long.class)
                return 0L;
            if (type == double.class)
                return 0.0;
            return null;
        }

        if (type == String.class)
            return data.asString();
        if (type == boolean.class || type == Boolean.class) {
            if (data.getValue() instanceof Boolean b)
                return b;
            return Boolean.parseBoolean(data.asString());
        }
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(data.asString());
        if (type == long.class || type == Long.class)
            return Long.parseLong(data.asString());

        return data.getValue();
    }
}
