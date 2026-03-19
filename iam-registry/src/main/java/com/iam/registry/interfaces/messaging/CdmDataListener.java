package com.iam.registry.interfaces.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.registry.application.UserRegistryService;
import com.iam.registry.domain.user.IamUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdmDataListener {

    private final UserRegistryService userRegistryService;
    private final ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "CDM_DATA_QUEUE", durable = "true"), exchange = @Exchange(value = "iam.exchange", type = "topic"), key = "iam.routing.cdm"))
    public void onCdmDataReceived(Map<String, Object> payloadMap) {
        log.info("Received CDM Data to persist in registry: {}", payloadMap);
        try {
            Map<String, Object> data = (Map<String, Object>) payloadMap.get("payload");
            if (data != null) {
                IamUser user = new IamUser();
                user.setUserName((String) data.get("userName"));
                user.setExternalId((String) data.get("externalId"));

                Object activeObj = data.get("active");
                if (activeObj != null) {
                    user.setActive(Boolean.parseBoolean(activeObj.toString()));
                }

                userRegistryService.saveUser(user);
                log.info("Successfully persisted user: {}", user.getUserName());
            }
        } catch (Exception e) {
            log.error("Failed to parse and save CDM data", e);
        }
    }
}
