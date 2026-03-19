package com.iam.engine.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class GroovyScriptEngineServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private CompilerConfiguration groovyCompilerConfig = new CompilerConfiguration();

    @InjectMocks
    private GroovyScriptEngineService scriptEngineService;

    // 실제 RabbitTemplate 연동 검증은 통합 테스트에서 하거나 Mock 처리
    // 여기서는 Groovy 매핑이 제대로 돌아가는지만 검증합니다.

    @Test
    @DisplayName("입력된 Raw Data가 Groovy Script에 의해 정상 변환되는지 검증")
    void test_executeScriptAndTransform() {
        // given
        String script = """
                def result = [:]
                result.put('targetField', source.sourceField)
                result.put('active', source.status == 'A')
                return result
                """;

        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("sourceField", "gildong.hong");
        sourceData.put("status", "A");

        Map<String, Object> params = new HashMap<>();
        params.put("source", sourceData);

        // when
        Object transformed = scriptEngineService.execute(script, "rules", params);

        // then
        assertNotNull(transformed);
        Map<String, Object> resultMap = (Map<String, Object>) transformed;

        assertEquals("gildong.hong", resultMap.get("targetField"));
        assertEquals(true, resultMap.get("active"));
    }
}
