package com.iam.core.domain.common.exception;

import lombok.Getter;

@Getter
public class TransformationException extends RuleEngineException {
    private final String ruleId;
    private final long revId;

    public TransformationException(ErrorCode errorCode, String detail, String ruleId, long revId, Throwable cause) {
        super(errorCode, detail, cause);
        this.ruleId = ruleId;
        this.revId = revId;
    }

    public TransformationException(String detail, long revId, Throwable cause) {
        super(ErrorCode.TRANS_RULE_ERROR, detail, cause);
        this.ruleId = "";
        this.revId = revId;
    }
}
