package com.example.shortener.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class KafkaConfig {

    @Bean
    ProducerFactory<String, ClickMessage> clickProducerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        DefaultKafkaProducerFactory<String, ClickMessage> factory = new DefaultKafkaProducerFactory<>(props);
        JsonSerializer<ClickMessage> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(false);
        factory.setValueSerializer(serializer);
        return factory;
    }

    @Bean
    KafkaTemplate<String, ClickMessage> clickKafkaTemplate(
            ProducerFactory<String, ClickMessage> clickProducerFactory
    ) {
        return new KafkaTemplate<>(clickProducerFactory);
    }

    @Bean
    ConsumerFactory<String, ClickMessage> clickConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<ClickMessage> deserializer = new JsonDeserializer<>(ClickMessage.class, objectMapper);
        deserializer.addTrustedPackages("com.example.shortener.messaging");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ClickMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, ClickMessage> clickConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ClickMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(clickConsumerFactory);
        return factory;
    }
}
