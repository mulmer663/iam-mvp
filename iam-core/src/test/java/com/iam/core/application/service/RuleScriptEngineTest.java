package com.iam.core.application.service;

import com.iam.core.config.GroovySandboxConfig;
import com.iam.core.domain.vo.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RuleScriptEngine.class)
@Import(GroovySandboxConfig.class)
class RuleScriptEngineTest {

    @Autowired
    private RuleScriptEngine ruleScriptEngine;

    private Map<String, Object> source;
    private Map<String, Object> target;
    private Map<String, Object> params;

    @BeforeEach
    void setUp() {
        source = new HashMap<>();
        target = new HashMap<>();
        params = new HashMap<>();
        params.put("source", source);
        params.put("target", target);
    }

    @Test
    @DisplayName("Splitting a full name into first and last name")
    void testNameSplit() {
        // Given
        source.put("fullName", new StringData("John Doe"));
        String script = """
                    def full = source.fullName.asString()
                    def parts = full.split(" ")
                    return [
                        givenName: new com.iam.core.domain.vo.StringData(parts[0]),
                        familyName: new com.iam.core.domain.vo.StringData(parts[1])
                    ]
                """;

        // When
        Object result = ruleScriptEngine.execute(script, "hash1", params);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, UniversalData> resultMap = (Map<String, UniversalData>) result;
        assertThat(resultMap.get("givenName").asString()).isEqualTo("John");
        assertThat(resultMap.get("familyName").asString()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Conditional logic based on employee grade")
    void testConditionalMapping() {
        // Given
        source.put("grade", new IntData(7));
        String script = """
                    def gradeVal = source.grade.getValue()
                    def title = (gradeVal >= 5) ? "Senior" : "Junior"
                    return [
                        jobTitle: new com.iam.core.domain.vo.StringData(title)
                    ]
                """;

        // When
        Object result = ruleScriptEngine.execute(script, "hash2", params);

        // Then
        Map<String, UniversalData> resultMap = (Map<String, UniversalData>) result;
        assertThat(resultMap.get("jobTitle").asString()).isEqualTo("Senior");
    }

    @Test
    @DisplayName("Content-based transformation (Like/Contains)")
    void testEmailDomainTransformation() {
        // Given
        source.put("email", new StringData("user@company.com"));
        String script = """
                    def emailStr = source.email.asString()
                    def isCorp = emailStr.endsWith("@company.com")
                    return [
                        isInternal: new com.iam.core.domain.vo.BooleanData(isCorp)
                    ]
                """;

        // When
        Object result = ruleScriptEngine.execute(script, "hash3", params);

        // Then
        Map<String, UniversalData> resultMap = (Map<String, UniversalData>) result;
        assertThat(resultMap.get("isInternal").getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Complex logic: Split, Check, and Map")
    void testComplexTransformation() {
        // Given
        source.put("rawDept", new StringData("IT-DEVELOPMENT-SEOUL"));
        String script = """
                    def deptRaw = source.rawDept.asString()
                    def parts = deptRaw.split("-")

                    def result = [:]
                    result.dept = new com.iam.core.domain.vo.StringData(parts[0])
                    result.team = new com.iam.core.domain.vo.StringData(parts[1])

                    if (parts[2] == "SEOUL") {
                        result.location = new com.iam.core.domain.vo.StringData("HQ")
                    } else {
                        result.location = new com.iam.core.domain.vo.StringData("Remote")
                    }
                    return result
                """;

        // When
        Object result = ruleScriptEngine.execute(script, "hash4", params);

        // Then
        Map<String, UniversalData> resultMap = (Map<String, UniversalData>) result;
        assertThat(resultMap.get("dept").asString()).isEqualTo("IT");
        assertThat(resultMap.get("team").asString()).isEqualTo("DEVELOPMENT");
        assertThat(resultMap.get("location").asString()).isEqualTo("HQ");
    }
}
