package com.iam.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamRabbitConfig {

    public static final String EXCHANGE_NAME = "iam.topic";
    public static final String INGEST_QUEUE_NAME = "q.iam.core.ingest";
    public static final String PROVISION_QUEUE_NAME = "q.iam.connector.ad"; // Created here or in connector? Better here
                                                                            // to ensure existence.

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue ingestQueue() {
        return new Queue(INGEST_QUEUE_NAME, true);
    }

    @Bean
    public Queue provisionQueue() {
        return new Queue(PROVISION_QUEUE_NAME, true);
    }

    @Bean
    public Binding ingestBinding(Queue ingestQueue, TopicExchange iamExchange) {
        return BindingBuilder.bind(ingestQueue).to(iamExchange).with("hr.event.#");
    }

    @Bean
    public Binding provisionBinding(Queue provisionQueue, TopicExchange iamExchange) {
        return BindingBuilder.bind(provisionQueue).to(iamExchange).with("cmd.ad.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
