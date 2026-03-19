package com.iam.registry.domain.common.revision;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "iam_rev_info") // 기본 ?�이�??�름???�용?�거??변�?가??
@RevisionEntity(UserRevisionListener.class) // ?�래?�서 만들 리스??지??
@Getter
@Setter
public class CustomRevisionEntity extends DefaultRevisionEntity {
    private String traceId;    // ?�랜??�� ?�이??
    private String operatorId;  // ?�업???�이??
    private String operationType; // 비즈?�스?�인 ?�업 분류
    private LocalDateTime createdAt;
}
