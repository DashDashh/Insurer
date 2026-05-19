package com.projectci.insurance.consumer;

import com.projectci.insurance.model.MessageResponse;
import com.projectci.insurance.service.InsuranceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
@Slf4j
public class KafkaAnalyticsResponseConsumer {
    private final InsuranceService insuranceService;

    public KafkaAnalyticsResponseConsumer(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @KafkaListener(
            topics = "component.analytics.response",
            groupId = "insurance-analytics-responses",
            containerFactory = "analyticsResponseKafkaListenerContainerFactory"
    )
    public void consumeAnalyticsResponse(MessageResponse message) {
        log.info("Received analytics response via Kafka: {}", message);
        log.info("Payload type: {}, Success: {}",
                message.getPayload() != null ? message.getPayload().getClass() : "null",
                message.isSuccess());

        try {
            insuranceService.processAnalyticsResponse(message);
        } catch (Exception e) {
            log.error("Error processing analytics response", e);
        }
    }
}
