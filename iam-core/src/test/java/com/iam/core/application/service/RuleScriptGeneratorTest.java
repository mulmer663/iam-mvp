package com.iam.core.application.service;

import com.iam.core.application.sync.RuleScriptGenerator;
import com.iam.core.domain.sync.TransCodeValue;
import com.iam.core.domain.sync.TransCodeValueRepository;
import com.iam.core.domain.sync.TransFieldMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleScriptGeneratorTest {

    private final TransCodeValueRepository codeValueRepository = mock(TransCodeValueRepository.class);
    private final RuleScriptGenerator generator = new RuleScriptGenerator(codeValueRepository);

    @Test
    @DisplayName("Verify CUSTOM conditional logic script generation")
    void testCustomConditionMapping() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("grade")
                .targetField("title")
                .transformType("CUSTOM")
                .transformScript("(source.grade?.asInt() ?: 0) >= 5 ? 'Senior' : 'Junior'")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThat(script).contains("res.title = (source.grade?.asInt() ?: 0) >= 5 ? 'Senior' : 'Junior'");
    }

    @Test
    @DisplayName("Verify DIRECT mapping script generation")
    void testDirectMapping() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("empNo")
                .targetField("employeeNumber")
                .transformType("DIRECT")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThat(script)
                .contains("res.employeeNumber = new com.iam.core.domain.vo.StringData(source.empNo?.asString())");
    }

    @Test
    @DisplayName("Verify CODE mapping script generation")
    void testCodeMapping() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("grade")
                .targetField("grade")
                .transformType("CODE")
                .transformParams("A:1;B:2")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThat(script).contains("def map_grade = [:]");
        assertThat(script).contains("map_grade['A'] = '1'");
        assertThat(script).contains("map_grade['B'] = '2'");
        assertThat(script).contains(
                "res.grade = new com.iam.core.domain.vo.StringData(map_grade.get(source.grade?.asString()) ?: source.grade?.asString())");
    }

    @Test
    @DisplayName("Verify DB-driven CODE mapping script generation")
    void testDbCodeMapping() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("rank")
                .targetField("rank")
                .transformType("CODE")
                .codeGroupId("RANK_MAP")
                .build();

        TransCodeValue v1 = TransCodeValue.builder().sourceValue("A").targetValue("1").build();
        TransCodeValue v2 = TransCodeValue.builder().sourceValue("B").targetValue("2").build();
        when(codeValueRepository.findByCodeGroupId("RANK_MAP")).thenReturn(List.of(v1, v2));

        String script = generator.generate(List.of(mapping));

        assertThat(script).contains("def map_rank = [:]");
        assertThat(script).contains("map_rank['A'] = '1'");
        assertThat(script).contains("map_rank['B'] = '2'");
        assertThat(script).contains(
                "res.rank = new com.iam.core.domain.vo.StringData(map_rank.get(source.rank?.asString()) ?: source.rank?.asString())");
    }

    @Test
    @DisplayName("Verify CLASSIFY mapping script generation")
    void testClassifyMapping() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("deptName")
                .targetField("category")
                .transformType("CLASSIFY")
                .transformParams("IT:TECH;HR:CORP")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThat(script).contains("if (val_category?.contains('IT')) res_category = 'TECH'");
        assertThat(script).contains("if (val_category?.contains('HR')) res_category = 'CORP'");
        assertThat(script).contains("res.category = new com.iam.core.domain.vo.StringData(res_category)");
    }

    @Test
    @DisplayName("Verify ACTIVE field special handling")
    void testActiveField() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("status")
                .targetField("active")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThat(script).contains(
                "res.active = source.active != null ? source.active : new com.iam.core.domain.vo.BooleanData(true)");
    }
}
