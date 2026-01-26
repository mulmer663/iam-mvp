package com.iam.core.config.exception;

import com.iam.core.domain.common.exception.BaseIamException;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.util.TraceIdExtractor;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component("messageExceptionHandler")
public class MessageExceptionHandler implements RabbitListenerErrorHandler {

    private final TraceIdExtractor traceIdExtractor;

    public MessageExceptionHandler(TraceIdExtractor traceIdExtractor) {
        this.traceIdExtractor = traceIdExtractor;
    }

    @Override
    public Object handleError(Message amqpMessage, Channel channel, org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception) throws Exception {

        String traceId = traceIdExtractor.extract(message);
        Throwable cause = exception.getCause();

        if (cause instanceof ConstraintViolationException) {
            handleValidationError(traceId, (ConstraintViolationException) cause);
            return null; // DLQ로 전송하지 않음 (무효한 메시지)
        }

        if (cause instanceof BaseIamException) {
            handleBusinessError(traceId, (BaseIamException) cause);
        } else {
            handleUnexpectedError(traceId, cause);
        }

        // 재처리를 위해 예외를 다시 던짐
        throw exception;
    }

    private void handleValidationError(String traceId, ConstraintViolationException ex) {
        log.error("Message validation failed - traceId: {}, violations: {}",
                traceId, ex.getConstraintViolations());

        // 메트릭 수집, 알림 등 추가 처리
        recordErrorMetric(ErrorCode.VALIDATION_FAILED, traceId);
    }

    private void handleBusinessError(String traceId, BaseIamException ex) {
        if (ex.getErrorCode().getHttpStatus().is5xxServerError()) {
            log.error("Message processing server error - traceId: {}, errorCode: {}",
                    traceId, ex.getErrorCode().getCode(), ex);
        } else {
            log.warn("Message processing business error - traceId: {}, errorCode: {}, message: {}",
                    traceId, ex.getErrorCode().getCode(), ex.getMessage());
        }

        recordErrorMetric(ex.getErrorCode(), traceId);
    }

    private void handleUnexpectedError(String traceId, Throwable ex) {
        log.error("Unexpected message processing error - traceId: {}", traceId, ex);
        recordErrorMetric(ErrorCode.MESSAGE_PROCESSING_ERROR, traceId);
    }

    private void recordErrorMetric(ErrorCode errorCode, String traceId) {
        // 메트릭 수집 로직 (Micrometer, Prometheus 등)
        log.debug("Recording error metric - errorCode: {}, traceId: {}", errorCode.getCode(), traceId);
    }

}