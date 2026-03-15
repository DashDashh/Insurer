package com.projectci.insurance.service;

import com.projectci.insurance.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
//@RequiredArgsConstructor
@Slf4j
public class InsuranceService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PolicyService policyService;
    private final IncidentService incidentService;
    private final KbmService kbmService;
    private final String responseTopic;

    public InsuranceService(
            KafkaTemplate<String, Object> kafkaTemplate,
            PolicyService policyService,
            IncidentService incidentService,
            KbmService kbmService,
            @Qualifier("insuranceResponseTopicName") String responseTopic) {  // Инъекция имени топика

        this.kafkaTemplate = kafkaTemplate;
        this.policyService = policyService;
        this.incidentService = incidentService;
        this.kbmService = kbmService;
        this.responseTopic = responseTopic;
    }

    public void processInsuranceRequest(InsuranceRequest request) {
        //log.info("Processing insurance request: {}", request);

        InsuranceResponse response = null;

        try {
            switch (request.getRequestType()) {
                case CALCULATION:
                    response = processCalculation(request);
                    break;
                case PURCHASE:
                    response = processPurchase(request);
                    break;
                case INCIDENT:
                    response = processIncident(request);
                    break;
                case POLICY_TERMINATION:
                    response = processPolicyTermination(request);
                    break;
                default:
                    response = createErrorResponse(request, "Unknown request type");
            }

            // Отправка ответа в Kafka
            kafkaTemplate.send(responseTopic, response.getOrderId(), response);
            //log.info("Response sent: {}", response);

        } catch (Exception e) {
            //log.error("Error processing request: {}", request, e);
            InsuranceResponse errorResponse = createErrorResponse(request, e.getMessage());
            kafkaTemplate.send(responseTopic, errorResponse.getOrderId(), errorResponse);
        }
    }

    private InsuranceResponse processCalculation(InsuranceRequest request) {
        // ОФ1 - Заглушка для расчёта
        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                .status(InsuranceResponse.ResponseStatus.SUCCESS)
                .calculatedCost(kbmService.calculatePolicyCost(request))
                .manufacturerKbm(kbmService.getManufacturerKbm(request.getManufacturerId()))
                .operatorKbm(kbmService.getOperatorKbm(request.getOperatorId()))
                .message("Расчёт выполнен успешно")
                .build();
    }

    private InsuranceResponse processPurchase(InsuranceRequest request) {
        // ОФ2 - Покупка полиса
        Policy policy = policyService.createPolicy(request);

        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                .policyId(policy.getId())
                .status(InsuranceResponse.ResponseStatus.SUCCESS)
                .policyStartDate(policy.getStartDate())
                .policyEndDate(policy.getEndDate())
                .calculatedCost(policy.getCost())
                .message("Полис успешно оформлен")
                .build();
    }

    private InsuranceResponse processIncident(InsuranceRequest request) {
        // ОФ4 - Обработка инцидента
        Incident incident = request.getIncident();
        if (incident == null) {
            return createErrorResponse(request, "Incident data is missing");
        }

        // Обработка инцидента
        Incident processedIncident = incidentService.processIncident(incident);

        // Пересчёт КБМ (ОФ5)
        KbmCalculation manufacturerKbm = kbmService.recalculateKbm(
                request.getManufacturerId(), "MANUFACTURER", processedIncident);
        KbmCalculation operatorKbm = kbmService.recalculateKbm(
                request.getOperatorId(), "OPERATOR", processedIncident);

        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                .status(InsuranceResponse.ResponseStatus.SUCCESS)
                .coverageAmount(processedIncident.getDamageAmount())
                .paymentAmount(processedIncident.getDamageAmount()) // Заглушка
                .newManufacturerKbm(manufacturerKbm.getNewKbm())
                .newOperatorKbm(operatorKbm.getNewKbm())
                .message("Инцидент обработан, произведена выплата")
                .build();
    }

    private InsuranceResponse processPolicyTermination(InsuranceRequest request) {
        // ОФ3 - Автоматическое прекращение действия полиса
        boolean terminated = policyService.terminatePolicyByOrderId(request.getOrderId());

        if (terminated) {
            return InsuranceResponse.builder()
                    .responseId(UUID.randomUUID().toString())
                    .requestId(request.getRequestId())
                    .orderId(request.getOrderId())
                    .status(InsuranceResponse.ResponseStatus.SUCCESS)
                    .message("Полис успешно прекращён")
                    .build();
        } else {
            return createErrorResponse(request, "Policy not found or already terminated");
        }
    }

    private InsuranceResponse createErrorResponse(InsuranceRequest request, String errorMessage) {
        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request != null ? request.getRequestId() : null)
                .orderId(request != null ? request.getOrderId() : null)
                .status(InsuranceResponse.ResponseStatus.FAILED)
                .message("Error: " + errorMessage)
                .build();
    }
}
