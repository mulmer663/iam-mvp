package com.iam.core.domain.user;

import com.iam.core.domain.common.ExtensionData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enterprise User Extension (URN:
 * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User)
 * 정형화된 필드를 제공하여 'Map' 중첩 없이 직접 접근 가능하게 합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseUserExtension extends ExtensionData {
    private String employeeNumber;
    private String department;
    private String costCenter;
    private String organization;
    private String division;
    // private String manager; // 추후 Resource Reference로 구현
}
