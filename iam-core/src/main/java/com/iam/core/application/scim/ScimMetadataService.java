package com.iam.core.application.scim;

import com.iam.core.application.common.ScimResourceTypeDto;
import com.iam.core.application.common.ScimResourceTypeResponse;
import com.iam.core.application.common.ScimSchemaDto;
import com.iam.core.application.common.ScimSchemaResponse;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.IamBusinessException;
import com.iam.core.domain.scim.ScimResourceTypeMeta;
import com.iam.core.domain.scim.ScimResourceTypeMetaRepository;
import com.iam.core.domain.scim.ScimSchemaMeta;
import com.iam.core.domain.scim.ScimSchemaMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScimMetadataService {

        private final ScimSchemaMetaRepository scimSchemaMetaRepository;
        private final ScimResourceTypeMetaRepository scimResourceTypeMetaRepository;
        private final ScimSchemaService scimSchemaService;
        private final ScimResourceTypeService scimResourceTypeService;

        public List<ScimSchemaResponse> getAllSchemas() {
                return scimSchemaMetaRepository.findAll().stream()
                                .map(this::toSchemaResponse)
                                .toList();
        }

        public ScimSchemaResponse getSchema(String uri) {
                return scimSchemaMetaRepository.findById(uri)
                                .map(this::toSchemaResponse)
                                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                                                "Schema not found: " + uri));
        }

        public List<ScimResourceTypeResponse> getAllResourceTypes() {
                return scimResourceTypeMetaRepository.findAll().stream()
                                .map(this::toResourceTypeResponse)
                                .toList();
        }

        public ScimResourceTypeResponse getResourceType(String id) {
                return scimResourceTypeMetaRepository.findById(id)
                                .map(this::toResourceTypeResponse)
                                .orElseThrow(() -> new IamBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "SCIM",
                                                "ResourceType not found: " + id));
        }

        private ScimSchemaResponse toSchemaResponse(ScimSchemaMeta meta) {
                ScimSchemaDto dto = scimSchemaService.mapToDto(meta);
                return ScimSchemaResponse.builder()
                                .id(dto.id())
                                .name(dto.name())
                                .description(dto.description())
                                .attributes(dto.attributes() != null ? dto.attributes().stream()
                                                .map(this::toAttributeResponse)
                                                .toList() : null)
                                .build();
        }

        private ScimSchemaResponse.ScimAttribute toAttributeResponse(ScimSchemaDto.AttributeDto dto) {
                return ScimSchemaResponse.ScimAttribute.builder()
                                .name(dto.name())
                                .type(dto.type())
                                .multiValued(dto.multiValued())
                                .description(dto.description())
                                .required(dto.required())
                                .mutability(dto.mutability())
                                .returned(dto.returned())
                                .uniqueness(dto.uniqueness())
                                .subAttributes(dto.subAttributes() != null ? dto.subAttributes().stream()
                                                .map(this::toAttributeResponse)
                                                .toList() : null)
                                .build();
        }

        private ScimResourceTypeResponse toResourceTypeResponse(ScimResourceTypeMeta meta) {
                ScimResourceTypeDto dto = scimResourceTypeService.mapToDto(meta);
                return ScimResourceTypeResponse.builder()
                                .id(dto.id())
                                .name(dto.name())
                                .description(dto.description())
                                .endpoint(dto.endpoint())
                                .schema(dto.schema())
                                .schemaExtensions(dto.schemaExtensions().stream()
                                                .map(ext -> ScimResourceTypeResponse.SchemaExtension.builder()
                                                                .schema(ext.schema())
                                                                .required(ext.required())
                                                                .build())
                                                .toList())
                                .build();
        }
}
