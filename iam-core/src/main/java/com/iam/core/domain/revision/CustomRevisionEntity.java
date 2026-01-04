package com.iam.core.domain.revision;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "iam_rev_info") // 기본 테이블 이름을 사용하거나 변경 가능
@RevisionEntity(UserRevisionListener.class) // 아래에서 만들 리스너 지정
@Getter
@Setter
public class CustomRevisionEntity extends DefaultRevisionEntity {
    private String traceId;    // 트랜잭션 아이디
    private String operatorId;  // 작업자 아이디
    private String operationType; // 비즈니스적인 작업 분류
    private LocalDateTime createdAt;
}
