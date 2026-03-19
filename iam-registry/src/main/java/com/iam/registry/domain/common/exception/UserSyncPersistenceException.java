package com.iam.registry.domain.common.exception;

import lombok.Getter;

@Getter
public class UserSyncPersistenceException extends BaseIamException {
    private final Long iamUserId;

    public UserSyncPersistenceException(ErrorCode errorCode, String traceId, String message,
                                        Long iamUserId, Throwable cause) {
        super(errorCode, traceId, message, cause);
        this.iamUserId = iamUserId;
    }
}
