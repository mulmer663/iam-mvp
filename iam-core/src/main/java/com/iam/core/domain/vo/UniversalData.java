package com.iam.core.domain.vo;

/**
 * Universal Data Abstraction for HR Integration.
 * 
 * <strong> Don't move this interface to another package. Don't change the name
 * of this interface. </strong>
 */
public sealed interface UniversalData permits StringData, IntData, TimeData, BooleanData {
    Object getValue();

    String asString();
}
