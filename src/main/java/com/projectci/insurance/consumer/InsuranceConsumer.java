package com.projectci.insurance.consumer;

import com.projectci.insurance.model.InsuranceRequest;
import com.projectci.insurance.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceConsumer {

    private final InsuranceService insuranceService;

    @KafkaListener(topics = "insurance-requests", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeInsuranceRequest(InsuranceRequest request) {
        //log.info("Received insurance request: {}", request);

        try {
            insuranceService.processInsuranceRequest(request);
        } catch (Exception e) {
            //log.error("Error processing insurance request: {}", request, e);
        }
    }
}