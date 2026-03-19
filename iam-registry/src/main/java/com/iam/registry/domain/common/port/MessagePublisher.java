
package com.iam.registry.domain.common.port;

/**
 * 메시지 발행???�한 ?�트 ?�터?�이??
 * ?�린 ?�키?�처 ?�칙???�라 Domain Layer?�서 Infrastructure Layer??구체?�인 구현???�존?��? ?�도�???
 */
public interface MessagePublisher {

    /**
     * 지?�된 ?�우???�로 메시지�?발행?�니??
     *
     * @param routingKey 메시지 ?�우????
     * @param message 발행??메시지 객체
     */
    void publish(String routingKey, Object message);

    /**
     * 지?�된 Exchange?� ?�우???�로 메시지�?발행?�니??
     *
     * @param exchange Exchange ?�름
     * @param routingKey 메시지 ?�우???? 
     * @param message 발행??메시지 객체
     */
    void publish(String exchange, String routingKey, Object message);
}
