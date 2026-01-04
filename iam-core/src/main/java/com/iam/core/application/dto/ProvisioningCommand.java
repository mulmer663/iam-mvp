package com.iam.core.application.dto;

import java.util.Map;

public record ProvisioningCommand(
        String traceId,
        String causeEventId,
        String command, // e.g., "CREATE", "UPDATE", "ENABLE", "DISABLE"
        AdProvisioningPayload payload) {

        public record AdProvisioningPayload(
                // AD 핵심 식별자
                String sAMAccountName,      // AD 로그온 ID
                String userPrincipalName,   // e.g., user@domain.com
                String distinguishedName,   // e.g., CN=Name,OU=Users,DC=corp

                // 기본 속성 (AD LDAP Attribute 명칭 준수)
                String sn,                  // familyName
                String givenName,           // givenName
                String displayName,         // formattedName
                String title,               // 직함
                String mail,                // 이메일

                boolean enabled,            // AD 계정 활성화 여부

                // 확장 속성 (Custom Attributes)
                Map<String, Object> extensionAttributes) { // e.g., extensionAttribute1...15
        }
}
