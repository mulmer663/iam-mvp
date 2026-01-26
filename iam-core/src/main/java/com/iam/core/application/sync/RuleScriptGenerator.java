package com.iam.core.application.sync;

import com.iam.core.domain.sync.TransCodeValue;
import com.iam.core.domain.sync.TransCodeValueRepository;
import com.iam.core.domain.sync.TransFieldMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleScriptGenerator {

    private final TransCodeValueRepository codeValueRepository;

    public String generate(List<TransFieldMapping> mappings) {
        StringBuilder sb = new StringBuilder();
        sb.append("def res = [:]\n");

        for (TransFieldMapping mapping : mappings) {
            String source = "source." + mapping.getSourceField();
            String target = mapping.getTargetField();

            // Special case for 'active' field as requested
            if ("active".equalsIgnoreCase(target)) {
                sb.append(
                        "res.active = source.active != null ? source.active : new com.iam.core.domain.vo.BooleanData(true)\n");
                continue;
            }

            // Custom Script priority
            if ("CUSTOM".equalsIgnoreCase(mapping.getTransformType()) && mapping.getTransformScript() != null
                    && !mapping.getTransformScript().isBlank()) {
                sb.append("// Custom logic for ").append(target).append("\n");
                sb.append("res.").append(target).append(" = ").append(mapping.getTransformScript()).append("\n");
                continue;
            }

            // Pattern Handling
            sb.append("// Mapping ").append(mapping.getSourceField()).append(" -> ").append(target).append(" (Type: ")
                    .append(mapping.getTransformType()).append(")\n");

            String valueExpr = source + "?.asString()";
            String assignedValue = "new com.iam.core.domain.vo.StringData(" + valueExpr + ")";

            if ("CODE".equalsIgnoreCase(mapping.getTransformType())) {
                // Fetch from DB if codeGroupId is set
                if (mapping.getCodeGroupId() != null && !mapping.getCodeGroupId().isBlank()) {
                    List<TransCodeValue> codes = codeValueRepository.findByCodeGroupId(mapping.getCodeGroupId());
                    sb.append("def map_").append(target).append(" = [:]\n");
                    for (TransCodeValue cv : codes) {
                        sb.append("map_").append(target).append("['").append(cv.getSourceValue()).append("'] = '")
                                .append(cv.getTargetValue()).append("'\n");
                    }
                    assignedValue = "new com.iam.core.domain.vo.StringData(map_" + target + ".get(" + valueExpr
                            + ") ?: " + valueExpr + ")";
                } else {
                    // Fallback to transformParams if codeGroupId is missing
                    String params = mapping.getTransformParams() != null ? mapping.getTransformParams() : "";
                    sb.append("def map_").append(target).append(" = [:]\n");
                    for (String pair : params.split(";")) {
                        String[] kv = pair.split(":");
                        if (kv.length == 2) {
                            sb.append("map_").append(target).append("['").append(kv[0]).append("'] = '").append(kv[1])
                                    .append("'\n");
                        }
                    }
                    assignedValue = "new com.iam.core.domain.vo.StringData(map_" + target + ".get(" + valueExpr
                            + ") ?: " + valueExpr + ")";
                }
            } else if ("CLASSIFY".equalsIgnoreCase(mapping.getTransformType())) {
                // Example params: "pattern:result;..."
                String params = mapping.getTransformParams() != null ? mapping.getTransformParams() : "";
                sb.append("def val_").append(target).append(" = ").append(valueExpr).append("\n");
                sb.append("def res_").append(target).append(" = val_").append(target).append("\n");
                for (String pair : params.split(";")) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        sb.append("if (val_").append(target).append("?.contains('").append(kv[0]).append("')) res_")
                                .append(target).append(" = '").append(kv[1]).append("'\n");
                    }
                }
                assignedValue = "new com.iam.core.domain.vo.StringData(res_" + target + ")";
            } else if ("REPLACE".equalsIgnoreCase(mapping.getTransformType())) {
                // Example params: "A:B;C:D"
                String params = mapping.getTransformParams() != null ? mapping.getTransformParams() : "";
                sb.append("def rep_").append(target).append(" = [:]\n");
                for (String pair : params.split(";")) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        sb.append("rep_").append(target).append("['").append(kv[0]).append("'] = '").append(kv[1])
                                .append("'\n");
                    }
                }
                assignedValue = "new com.iam.core.domain.vo.StringData(rep_" + target + ".get(" + valueExpr + ") ?: "
                        + valueExpr + ")";
            }

            // Build the assignment with default value if needed
            if (mapping.getDefaultValue() != null) {
                // Apply default value to the raw result before wrapping in StringData
                String rawValueExpr = assignedValue.substring(assignedValue.indexOf("(") + 1,
                        assignedValue.lastIndexOf(")"));
                assignedValue = "new com.iam.core.domain.vo.StringData(" + rawValueExpr + " ?: \""
                        + mapping.getDefaultValue() + "\")";
            }

            // Validation logic
            if (Boolean.TRUE.equals(mapping.getIsRequired())) {
                sb.append("if (").append(source)
                        .append(" == null) throw new com.iam.core.domain.exception.RuleValidationException('")
                        .append(target)
                        .append(" is required')\n");
            }
            if (mapping.getMinLength() != null) {
                sb.append("if (").append(valueExpr).append("?.length() < ").append(mapping.getMinLength())
                        .append(") throw new com.iam.core.domain.exception.RuleValidationException('").append(target)
                        .append(" too short')\n");
            }
            if (mapping.getMaxLength() != null) {
                sb.append("if (").append(valueExpr).append("?.length() > ").append(mapping.getMaxLength())
                        .append(") throw new com.iam.core.domain.exception.RuleValidationException('").append(target)
                        .append(" too long')\n");
            }

            sb.append("res.").append(target).append(" = ").append(assignedValue).append("\n");
        }

        sb.append("return res\n");
        return sb.toString();
    }
}
