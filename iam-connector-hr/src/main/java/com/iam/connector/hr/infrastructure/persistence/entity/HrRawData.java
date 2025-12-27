package com.iam.connector.hr.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Simulates an external HR system table.
 */
@Entity
@Table(name = "HR_RAW_DATA_MOCK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrRawData {

    @Id
    @Column(name = "EMP_NO", length = 50)
    private String empNo;

    @Column(name = "FIRST_NAME", length = 100)
    private String firstName;

    @Column(name = "LAST_NAME", length = 100)
    private String lastName;

    @Column(name = "POSITION", length = 100)
    private String position;

    @Column(name = "DEPT_CODE", length = 50)
    private String deptCode;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "STATUS", length = 20)
    private String status; // ACTIVE, INACTIVE
}
