package com.iam.core.adapter.messaging;

import com.iam.core.application.dto.UserSyncEvent;
import com.iam.core.application.service.UserSyncService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * RabbitMQ 메시지를 수신하여 Service Layer로 위임하는 Adapter
 * Infrastructure Layer - 메시징 기술에 의존
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class UserSyncMessageHandler {

    private final UserSyncService userSyncService;

    @RabbitListener(queues = "q.iam.core.ingest", errorHandler = "messageExceptionHandler")
    public void handleUserSyncMessage(@Valid UserSyncEvent event) {
        log.info("Received message from queue - traceId: {}, eventType: {}",
                event.traceId(), event.eventType());

        userSyncService.processHrSync(event);
        log.info("Successfully processed sync event - traceId: {}", event.traceId());
    }
}