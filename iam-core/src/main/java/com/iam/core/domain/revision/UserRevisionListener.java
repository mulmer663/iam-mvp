package com.iam.core.domain.revision;

import io.hypersistence.tsid.TSID;
import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;

import java.time.LocalDateTime;

public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;

        // MDC에서 값 추출
        String traceId = MDC.get("traceId");
        String operationType = MDC.get("operationType");

        rev.setTraceId(traceId != null ? traceId : "T_" + TSID.fast().toLong());
        rev.setOperationType(operationType != null ? operationType : "SYSTEM_EVENT");

        // 작업자 정보 (보통은 인증 객체에서 가져오거나 시스템 상수를 사용)
        rev.setOperatorId("IAM_USER");

        // 시간 저장
        rev.setCreatedAt(LocalDateTime.now());
    }
}