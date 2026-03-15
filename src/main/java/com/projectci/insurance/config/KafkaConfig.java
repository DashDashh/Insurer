package com.projectci.insurance.config;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    //@Value("${INSTANCE_ID}")
    private String instanceId;

    @PostConstruct
    public void init() {
        // Получаем INSTANCE_ID из переменной окружения
        this.instanceId = System.getenv("INSTANCE_ID");
        if (this.instanceId == null || this.instanceId.isEmpty()) {
            this.instanceId = "1"; // значение по умолчанию
        }
        System.out.println("=== KafkaConfig: instanceId = " + this.instanceId + " ===");
    }

    public String getInsuranceRequestTopicName() {
        return String.format("v1.%s.%s.%s.requests", "Insurer", instanceId, "insurer-service");
    }
    @Bean
    public String insuranceRequestTopicName() {
        return getInsuranceRequestTopicName();
    }

    public String getInsuranceResponseTopicName() {
        return String.format("v1.%s.%s.%s.responses", "Insurer", instanceId, "insurer-service");
    }
    @Bean
    public String insuranceResponseTopicName() {
        return getInsuranceResponseTopicName();
    }

    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.projectci.insurance.model");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, "com.projectci.insurance.model.InsuranceRequest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // Topics
    @Bean
    public NewTopic insuranceRequestTopic() {
        return new NewTopic(getInsuranceRequestTopicName(), 3, (short) 1);
    }

    @Bean
    public NewTopic insuranceResponseTopic() {
        return new NewTopic(getInsuranceResponseTopicName(), 3, (short) 1);
    }

    @Bean
    public NewTopic incidentTopic() {
        return new NewTopic("incident-reports", 1, (short) 1);
    }
}