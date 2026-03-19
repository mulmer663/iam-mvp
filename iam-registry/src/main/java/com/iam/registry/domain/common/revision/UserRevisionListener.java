package com.iam.registry.domain.common.revision;

import io.hypersistence.tsid.TSID;
import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;

import java.time.LocalDateTime;

public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        // MDC?�서 �?추출
        String traceId = MDC.get("traceId");
        String operationType = MDC.get("operationType");

        rev.setTraceId(traceId != null ? traceId : "T_" + TSID.fast().toLong());
        rev.setOperationType(operationType != null ? operationType : "SYSTEM_EVENT");

        // ?�업???�보 (보통?� ?�증 객체?�서 가?�오거나 ?�스???�수�??�용)
        rev.setOperatorId("IAM_USER");

        // ?�간 ?�??
        rev.setCreatedAt(LocalDateTime.now());
    }
}
