package com.iam.engine.interfaces.messaging;

import com.iam.engine.application.GroovyScriptEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineDataListener {

    private final GroovyScriptEngineService scriptEngineService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queuesToDeclare = @Queue("RAW_INBOUND_DATA"))
    public void onRawDataReceived(Map<String, Object> payload) {
        log.info("Engine received raw inbound data: {}", payload);

        // 1. In a real scenario, fetch the script content/hash from a cache or an
        // internal API based on payload metadata
        String dummyScript = "target.putAll(source); return target;";
        String dummyHash = "hash123";

        // 2. Prepare bindings
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("source", payload);
        bindings.put("target", new HashMap<>());

        // 3. Execute script
        Object transformedData = scriptEngineService.execute(dummyScript, dummyHash, bindings);

        log.info("Transformation successful: {}", transformedData);

        // 4. Send to CDM queue for registry to persist
        rabbitTemplate.convertAndSend("CDM_DATA_QUEUE", transformedData);
    }
}
