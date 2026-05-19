package com.projectci.insurance.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectci.insurance.model.MessageResponse;
import com.projectci.insurance.service.InsuranceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

@Component
@Profile("mqtt")
@Slf4j
public class MqttAnalyticsResponseConsumer {

    private final InsuranceService insuranceService;
    private final ObjectMapper objectMapper;

    public MqttAnalyticsResponseConsumer(InsuranceService insuranceService, ObjectMapper objectMapper) {
        this.insuranceService = insuranceService;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttAnalyticsResponseInputChannel")
    public void consumeMqttAnalyticsResponse(String payload) {
        try {
            log.info("Received analytics response via MQTT: {}", payload);
            MessageResponse message = objectMapper.readValue(payload, MessageResponse.class);
            insuranceService.processAnalyticsResponse(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize MQTT analytics response: {}", payload, e);
        } catch (Exception e) {
            log.error("Error processing MQTT analytics response", e);
        }
    }
}