package com.iam.core.domain.vo;

/**
 * Universal Data Abstraction for HR Integration.
 * 
 * <strong> Don't move this interface to another package. Don't change the name
 * of this interface. </strong>
 */
public sealed interface UniversalData permits StringData, IntData, TimeData, BooleanData, NullData {
    Object getValue();

    String asString();

    default Integer asInt() {
        return 0;
    }

    default Long asLong() {
        return 0L;
    }

    default Double asDouble() {
        return 0.0;
    }

    default Boolean asBoolean() {
        return false;
    }
}
