package com.iam.core.util;

import java.util.Map;

public class AttributeUtils {
    public static String getString(Map<String, Object> attrs, String key) {
        if (attrs == null || !attrs.containsKey(key)) {
            return "";
        }
        Object val = attrs.get(key);
        return val != null ? val.toString() : "";
    }
}
