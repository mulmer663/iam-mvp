
package com.iam.core.domain.common.port;

/**
 * 메시지 발행을 위한 포트 인터페이스
 * 클린 아키텍처 원칙에 따라 Domain Layer에서 Infrastructure Layer의 구체적인 구현에 의존하지 않도록 함
 */
public interface MessagePublisher {

    /**
     * 지정된 라우팅 키로 메시지를 발행합니다.
     *
     * @param routingKey 메시지 라우팅 키
     * @param message 발행할 메시지 객체
     */
    void publish(String routingKey, Object message);

    /**
     * 지정된 Exchange와 라우팅 키로 메시지를 발행합니다.
     *
     * @param exchange Exchange 이름
     * @param routingKey 메시지 라우팅 키  
     * @param message 발행할 메시지 객체
     */
    void publish(String exchange, String routingKey, Object message);
}