package com.projectci.insurance.consumer;

import com.projectci.insurance.model.InsuranceRequest;
import com.projectci.insurance.service.InsuranceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
@Slf4j
public class KafkaInsuranceConsumer {

    private final InsuranceService insuranceService;

    public KafkaInsuranceConsumer(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @KafkaListener(topics = "#{@insuranceRequestTopicName}")
    public void consumeKafka(InsuranceRequest request) {
        log.info("Received via Kafka: {}", request);
        try {
            insuranceService.processInsuranceRequest(request);
        } catch (Exception e) {
            log.error("Error processing Kafka request", e);
        }
    }
}
