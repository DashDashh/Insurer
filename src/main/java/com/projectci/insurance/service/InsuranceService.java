package com.projectci.insurance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectci.insurance.config.TopicConfig;
import com.projectci.insurance.model.*;
import com.projectci.insurance.model.analytics.AnalyticsMessage;
import com.projectci.insurance.model.analytics.CalcRequest;
import com.projectci.insurance.model.analytics.CalcResponse;
import com.projectci.insurance.producer.MessagePublisher;
import com.projectci.insurance.utils.NamespaceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
//@RequiredArgsConstructor
@Slf4j
public class InsuranceService {

    private final MessagePublisher messagePublisher;
    private final PolicyService policyService;
    private final IncidentService incidentService;
    private final KbmService kbmService;
    private final TopicConfig topicConfig;
    private final NamespaceUtils namespaceUtils;
    private final String systemId;
    private final ObjectMapper objectMapper;
    Map<String,String> correlationResponseTopic = new HashMap<>();

    public InsuranceService(
            MessagePublisher messagePublisher,
            PolicyService policyService,
            IncidentService incidentService,
            KbmService kbmService,
            TopicConfig topicConfig,
            NamespaceUtils namespaceUtils,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:insurance-service}") String applicationName) {

        this.messagePublisher = messagePublisher;
        this.policyService = policyService;
        this.incidentService = incidentService;
        this.kbmService = kbmService;
        this.topicConfig = topicConfig;
        this.namespaceUtils = namespaceUtils;
        this.objectMapper = objectMapper;

        // Формируем уникальный ID отправителя с учетом namespace
        this.systemId = namespaceUtils.hasNamespace()
                ? namespaceUtils.getCurrentNamespace() + "." + applicationName
                : applicationName;
    }

    public void processInsuranceRequest(MessageRequest message) {
        log.info("Processing insurance request: {}, from system: {}",
                message.getAction(), systemId);

        Object payload = message.getPayload();
        InsuranceResponse response = null;
        AnalyticsMessage analyticsRequest = null;

        try {
            // Обработка запроса
            InsuranceRequest request = convertPayload(payload, InsuranceRequest.class);
            switch (message.getAction()) {
                case annual_insurance:
                    log.info("IN CASE ANNUAL", message.getAction(), systemId);
                    response = processPurchase(request, Policy.PolicyType.annual);
                    break;
                case mission_insurance:
                    log.info("IN CASE MISSION", message.getAction(), systemId);
                    response = processPurchase(request, Policy.PolicyType.mission);
                    break;
                case calculate_policy:
                    log.info("IN CASE CALC", message.getAction(), systemId);
                    analyticsRequest = processCalculation(request, message.getCorrelationId());
                    break;
                case purchase_policy:
                    log.info("IN CASE PURCHASE", message.getAction(), systemId);
                    response = processPurchase(request, Policy.PolicyType.annual);
                    break;
                case report_incident:
                    log.info("IN CASE REPORT", message.getAction(), systemId);
                    response = processIncident(request);
                    break;
                case terminate_policy:
                    log.info("IN CASE TERM", message.getAction(), systemId);
                    response = processPolicyTermination(request);
                    break;
                default:
                    response = createErrorResponse(request, "Unknown request type");
            }

            if (response != null) {
                // Определяем топик для ответа (с учетом namespace)
                // Ответ отправляем в системный топик отправителя или в компонентный топик
                String responseTopic = determineResponseTopic(message);

                // формируем сообщение
                MessageResponse messageOut = MessageResponse.createResponse(
                        message.getCorrelationId(), // correlationId
                        response,
                        true
                );

                // Отправка ответа
                messagePublisher.send(
                        responseTopic,
                        response.getOrderId(),
                        messageOut
                );
                logMessagePublish(responseTopic, messageOut.getCorrelationId());
            }
            else if (analyticsRequest != null) {
                messagePublisher.send(
                        "component.insurer_analytics",
                        analyticsRequest.getCorrelationId(),
                        analyticsRequest
                );
                correlationResponseTopic.put(message.getCorrelationId(), determineResponseTopic(message));
                logMessagePublish("component.insurer_analytics", analyticsRequest.getCorrelationId());
            }

        } catch (Exception e) {
            log.error("Error processing request: {}", payload, e);

            // Создаем сообщение об ошибке
            /*InsuranceResponse errorResponse = createErrorResponse(payload, e.getMessage());
            MessageResponse errorMessage = MessageResponse.createResponse(
                    payload.getRequestId(),
                    errorResponse,
                    false
            );*/

            // Отправляем в dead letters
            messagePublisher.send(
                    TopicConfig.DEAD_LETTERS_TOPIC,
                    null,
                    message
            );
        }
    }

    public void processAnalyticsResponse(MessageResponse message) {
        log.info("Processing analytics response: {}, from system: {}",
                message.getAction(), systemId);

        Object payload = message.getPayload();
        String action = message.getAction();
        InsuranceResponse response = null;

        try {
            switch (action) {
                case "CALCULATION_RESULT":
                    CalcResponse request = convertPayload(payload, CalcResponse.class);
                    response = processAnalyticsCalc(request);
                    break;
                case "INCIDENT_RESULT":
                    break;
                case "KBM_UPDATE_RESULT":
                    break;
                default:
                    // TODO: обработка неизвестного типа ответа
                    break;
            }
            if (response != null) {
                // Определяем топик для ответа (с учетом namespace)
                // Ответ отправляем сохраненный перед отправкой в аналитику топик для ответа
                String responseTopic = correlationResponseTopic.getOrDefault(message.getCorrelationId(), TopicConfig.DEAD_LETTERS_TOPIC);
                if (!responseTopic.equals(TopicConfig.DEAD_LETTERS_TOPIC)) {
                    correlationResponseTopic.remove(message.getCorrelationId());
                }

                // формируем сообщение
                MessageResponse messageOut = MessageResponse.createResponse(
                        message.getCorrelationId(), // correlationId
                        response,
                        true
                );

                // Отправка ответа
                messagePublisher.send(
                        responseTopic,
                        response.getOrderId(),
                        messageOut
                );
                logMessagePublish(responseTopic, messageOut.getCorrelationId());
            }
        }
        catch (Exception e) {
            log.error("Error processing request: {}", payload, e);

            // Отправляем в dead letters
            messagePublisher.send(
                    TopicConfig.DEAD_LETTERS_TOPIC,
                    null,
                    message
            );
        }
    }

    void logMessagePublish(String where, String corId) {
        log.info("Response sent to topic: {}, correlationId: {}",
                where, corId);
    }

    private <T> T convertPayload(Object payload, Class<T> targetClass) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(payload, targetClass);
    }

    /**
     * Определяет топик для ответа
     * Если отправитель указал конкретный топик - используем его
     * Иначе отправляем в системный топик отправителя
     */
    private String determineResponseTopic(MessageRequest message) {
        // Если в запросе указан топик для ответа
        if (message.getReplyTo() != null && !message.getReplyTo().isEmpty()) {
            return message.getReplyTo();
        }

        // Если указан отправитель - отправляем в его системный топик
        if (message.getSender() != null && !message.getSender().isEmpty()) {
            return topicConfig.getSystemTopic(message.getSender());
        }

        // По умолчанию отправляем в наш компонентный топик (для отладки)
        return topicConfig.getComponentTopic(
                TopicConfig.InsuranceSystem.Components.INSURANCE_SERVICE
        );
    }

    private AnalyticsMessage processCalculation(InsuranceRequest request, String correlationId) {
        // ОФ1 - обращение к страховой аналитике
        CalcRequest payload = new CalcRequest(
                UUID.randomUUID().toString(),
                request.getOrderId(),
                request.getManufacturerId(),
                request.getOperatorId(),
                request.getDroneId(),
                kbmService.getManufacturerKbm(request.getManufacturerId()),
                kbmService.getOperatorKbm(request.getOperatorId()),
                request.getSecurityGoals(),
                request.getSecurityGoals(),
                request.getCoverageAmount()
        );
        return new AnalyticsMessage(
                UUID.randomUUID().toString(),
                AnalyticsMessage.AnalyticsAction.CALCULATION,
                "insurance-service",
                "component.analytics.response",
                correlationId,
                System.currentTimeMillis(),
                payload,
                "request"
        );
        // старая заглушка для расчёта
        /*return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                //.status(InsuranceResponse.ResponseStatus.SUCCESS)
                .calculatedCost(kbmService.calculatePolicyCost(request))
                .coverageAmount(request.getCoverageAmount())
                .manufacturerKbm(kbmService.getManufacturerKbm(request.getManufacturerId()))
                .operatorKbm(kbmService.getOperatorKbm(request.getOperatorId()))
                .message("Расчёт выполнен успешно")
                .build();*/
    }

    private InsuranceResponse processPurchase(InsuranceRequest request, Policy.PolicyType type) {
        // ОФ2 - Покупка полиса
        Policy policy = policyService.createPolicy(request, type);

        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                .policyId(policy.getId())
                .policyType(type)
                .policyStatus(policy.getStatus())
                .droneId(policy.getDroneId())
                .droneKbm(policy.getDroneKbm())
                /*.status(InsuranceResponse.ResponseStatus.SUCCESS)*/
                .policyStartDate(policy.getStartDate())
                .policyEndDate(policy.getEndDate())
                .calculatedCost(policy.getCost())
                .coverageAmount(request.getCoverageAmount())
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
                /*.status(InsuranceResponse.ResponseStatus.SUCCESS)*/
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
                    /*.status(InsuranceResponse.ResponseStatus.SUCCESS)*/
                    .message("Полис успешно прекращён")
                    .build();
        } else {
            return createErrorResponse(request, "Policy not found or already terminated");
        }
    }

    private InsuranceResponse processAnalyticsCalc(CalcResponse request) {
        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .calculatedCost(request.getCalculatedCost())
                .coverageAmount(request.getCoverageAmount())
                .manufacturerKbm(kbmService.getManufacturerKbm(request.getManufacturerId()))
                .operatorKbm(kbmService.getOperatorKbm(request.getOperatorId()))
                .message("Расчёт выполнен успешно")
                .build();
    }

    private InsuranceResponse createErrorResponse(InsuranceRequest request, String errorMessage) {
        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request != null ? request.getRequestId() : null)
                .orderId(request != null ? request.getOrderId() : null)
                /*.status(InsuranceResponse.ResponseStatus.FAILED)*/
                .message("Error: " + errorMessage)
                .build();
    }
}
