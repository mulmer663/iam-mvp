package com.iam.registry.domain.user;

import com.iam.registry.domain.common.ExtensionData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enterprise User Extension (URN:
 * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User)
 * ?�형?�된 ?�드�??�공?�여 'Map' 중첩 ?�이 직접 ?�근 가?�하�??�니??
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
    // private String manager; // 추후 Resource Reference�?구현
}
