package com.iam.connector.hr.application.port.out;

import java.util.Map;

/**
 * Port for publishing HR events.
 */
public interface EventPublisher {
    void publish(String systemId, Map<String, Object> payload);
}
