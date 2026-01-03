package com.iam.core.application.service;

import com.iam.core.config.GroovySandboxConfig;
import com.iam.core.domain.entity.TransFieldMapping;
import com.iam.core.domain.repository.TransCodeValueRepository;
import com.iam.core.domain.vo.StringData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iam.core.domain.exception.RuleCompilationException;
import com.iam.core.domain.exception.RuleExecutionException;
import com.iam.core.domain.exception.RuleValidationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = { RuleScriptEngine.class, RuleScriptGenerator.class })
@Import(GroovySandboxConfig.class)
class RuleFailureIntegrationTest {

    @Autowired
    private RuleScriptEngine engine;

    @Autowired
    private RuleScriptGenerator generator;

    @MockitoBean
    private TransCodeValueRepository codeValueRepository;

    private Map<String, Object> params;
    private Map<String, Object> source;

    @BeforeEach
    void setUp() {
        source = new HashMap<>();
        params = new HashMap<>();
        params.put("source", source);
        params.put("target", new HashMap<>());
    }

    @Test
    @DisplayName("Failure Case 1: Syntax error in custom script")
    void testSyntaxError() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("name")
                .targetField("name")
                .transformType("CUSTOM")
                .transformScript("source.name.asString( // Missing parenthesis")
                .build();

        String script = generator.generate(List.of(mapping));

        assertThatThrownBy(() -> engine.execute(script, "syntax_error", params))
                .isInstanceOf(RuleCompilationException.class);
    }

    @Test
    @DisplayName("Failure Case 2: Validation violation (Length)")
    void testLengthViolation() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("code")
                .targetField("code")
                .minLength(5)
                .maxLength(10)
                .build();

        String script = generator.generate(List.of(mapping));

        // Too short
        source.put("code", new StringData("ABC"));
        assertThatThrownBy(() -> engine.execute(script, "len_short", params))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("code too short");

        // Too long
        source.put("code", new StringData("VERY_LONG_CODE_HERE"));
        assertThatThrownBy(() -> engine.execute(script, "len_long", params))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("code too long");
    }

    @Test
    @DisplayName("Failure Case 3: Validation violation (Required)")
    void testRequiredViolation() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("empNo")
                .targetField("empNo")
                .isRequired(true)
                .build();

        String script = generator.generate(List.of(mapping));

        // Missing source
        assertThatThrownBy(() -> engine.execute(script, "required_fail", params))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("empNo is required");
    }

    @Test
    @DisplayName("Failure Case 4: Data type violation (Numeric conversion fail)")
    void testTypeViolation() {
        TransFieldMapping mapping = TransFieldMapping.builder()
                .sourceField("grade")
                .targetField("title")
                .transformType("CUSTOM")
                .transformScript("source.grade.getValue() / 2")
                .build();

        String script = generator.generate(List.of(mapping));

        // String that is not a number
        source.put("grade", new StringData("NOT_A_NUMBER"));

        assertThatThrownBy(() -> engine.execute(script, "type_fail", params))
                .isInstanceOf(RuleExecutionException.class);
    }
}
