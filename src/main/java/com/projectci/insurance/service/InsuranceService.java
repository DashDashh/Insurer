package com.projectci.insurance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectci.insurance.config.TopicConfig;
import com.projectci.insurance.model.*;
import com.projectci.insurance.model.analytics.*;
import com.projectci.insurance.producer.MessagePublisher;
import com.projectci.insurance.utils.NamespaceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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
    Map<String, Policy.PolicyType> correlationPurePurchaseType = new HashMap<>();
    Map<String, InsuranceResponse> kbmCalcWaiting = new HashMap<>();
    Map<String, String> incidentsInProgress = new HashMap<>();

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
        List<AnalyticsMessage> analyticsRequest = null;
        Object result = null;

        try {
            // Обработка запроса
            InsuranceRequest request = convertPayload(payload, InsuranceRequest.class);
            switch (message.getAction()) {
                case annual_insurance:
                    log.info("IN CASE ANNUAL", message.getAction(), systemId);
                    result = processPurchase(request, Policy.PolicyType.annual, message.getCorrelationId());
                    break;
                case mission_insurance:
                    log.info("IN CASE MISSION", message.getAction(), systemId);
                    result = processPurchase(request, Policy.PolicyType.mission, message.getCorrelationId());
                    break;
                case calculate_policy:
                    log.info("IN CASE CALC", message.getAction(), systemId);
                    analyticsRequest = processCalculation(request, message.getCorrelationId());
                    break;
                case purchase_policy:
                    log.info("IN CASE PURCHASE", message.getAction(), systemId);
                    result = processPurchase(request, Policy.PolicyType.annual, message.getCorrelationId());
                    break;
                case report_incident:
                    log.info("IN CASE REPORT", message.getAction(), systemId);
                    result = processIncident(request, message.getCorrelationId());
                    break;
                case terminate_policy:
                    log.info("IN CASE TERM", message.getAction(), systemId);
                    response = processPolicyTermination(request);
                    break;
                default:
                    response = createErrorResponse(request, "Unknown request type");
            }

            if (result != null) {
                if (result instanceof InsuranceResponse) {
                    response = (InsuranceResponse) result;
                }
                /*else if (result instanceof AnalyticsMessage) {
                    analyticsRequest = convertPayload(result, AnalyticsMessage.class);
                }*/
                else if (result instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof AnalyticsMessage) {
                    analyticsRequest = (List<AnalyticsMessage>) result;
                }
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
                for (AnalyticsMessage msg : analyticsRequest) {
                    messagePublisher.send(
                            "component.insurer_analytics",
                            msg.getCorrelationId(),
                            msg
                    );
                    correlationResponseTopic.put(message.getCorrelationId(), determineResponseTopic(message));
                    logMessagePublish("component.insurer_analytics", msg.getCorrelationId());
                }
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
                    response = processAnalyticsCalc(request, message.getCorrelationId());
                    break;
                case "INCIDENT_RESULT":
                    log.info("IN CASE INCIDENT_RESULT");
                    IncidentResponse incidentResponse = convertPayload(payload, IncidentResponse.class);
                    response = processAnalyticsIncidentResponse(incidentResponse, message.getCorrelationId());
                    break;
                case "KBM_UPDATE_RESULT":
                    KbmResponse kbmResponse = convertPayload(payload, KbmResponse.class);
                    InsuranceResponse incompleted = kbmCalcWaiting.getOrDefault(message.getCorrelationId(), null);
                    String incidentId = incidentsInProgress.getOrDefault(message.getCorrelationId(), null);
                    if (incompleted != null) {
                        kbmCalcWaiting.remove(message.getCorrelationId());
                    }
                    if (incidentId != null) {
                        incidentsInProgress.remove(message.getCorrelationId());
                    }
                    if (incompleted != null && incidentId != null) {
                        response = processAnalyticsKbmUpdate(kbmResponse, incompleted, incidentId);
                    }
                    break;
                default:
                    log.error("Unknown request type: {}", message);

                    // Отправляем в dead letters
                    messagePublisher.send(
                            TopicConfig.DEAD_LETTERS_TOPIC,
                            null,
                            message
                    );
                    break;
            }
            if (response != null) {
                if (message.getAction().equals("INCIDENT_RESULT")){
                    kbmCalcWaiting.put(message.getCorrelationId(), response);
                }
                else {
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

    private List<AnalyticsMessage> processCalculation(InsuranceRequest request, String correlationId) {
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

        return List.of (
                new AnalyticsMessage(
                UUID.randomUUID().toString(),
                AnalyticsMessage.AnalyticsAction.CALCULATION,
                "insurance-service",
                "component.analytics.response",
                correlationId,
                System.currentTimeMillis(),
                payload,
                "request"
                )
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

    private Object processPurchase(InsuranceRequest request, Policy.PolicyType type, String correlationId) {
        // ОФ2 - Покупка полиса
        Optional<Policy> savedPolicy = policyService.getCalculatedPolicyForOrder(request.getOrderId());
        if (savedPolicy.isPresent()) {
            policyService.updatePolicyStatus(savedPolicy.get().getPolicyNumber(), Policy.PolicyStatus.active);
            Policy policy = savedPolicy.get();
            policy.setStatus(Policy.PolicyStatus.active);

            return InsuranceResponse.builder()
                    .responseId(UUID.randomUUID().toString())
                    .requestId(request.getRequestId())
                    .orderId(request.getOrderId())
                    .policyId(policy.getId())
                    .policyType(type)
                    .policyStatus(policy.getStatus())
                    .droneId(policy.getDroneId())
                    .droneKbm(policy.getDroneKbm())
                    .policyStartDate(policy.getStartDate())
                    .policyEndDate(policy.getEndDate())
                    .calculatedCost(policy.getCost())
                    .coverageAmount(request.getCoverageAmount())
                    .message("Полис успешно оформлен")
                    .build();
        }
        // запрос к аналитике
        correlationPurePurchaseType.put(correlationId, type);

        return processCalculation(request, correlationId);
    }

    private Object processIncident(InsuranceRequest request, String correlationId) {
        // ОФ4 - Обработка инцидента

        if (policyService.getActivePolicyForOrder(request.getOrderId()).isEmpty()) {
            return createErrorResponse(request, "There is no policy for order " + request.getOrderId());
        }

        Incident incident = request.getIncident();
        if (incident == null) {
            return createErrorResponse(request, "Incident data is missing");
        }
        if (incident.getManufacturerId() == null) incident.setManufacturerId(request.getManufacturerId());
        if (incident.getOperatorId() == null) incident.setOperatorId(request.getOperatorId());

        Incident processedIncident = incidentService.processIncident(incident);

        AnalyticsIncident analyticsIncident = AnalyticsIncident.fromInsuranceIncident(processedIncident);
        // payload запроса по инциденту
        IncidentRequest incidentRequest = new IncidentRequest(
                UUID.randomUUID().toString(),
                request.getOrderId(),
                request.getManufacturerId(),
                request.getOperatorId(),
                request.getDroneId(),
                request.getCoverageAmount(),
                analyticsIncident
        );

        // payload запроса по кбм
        BigDecimal manufacturerKbm = kbmService.getManufacturerKbm(request.getManufacturerId());
        BigDecimal operatorKbm = kbmService.getOperatorKbm(request.getOperatorId());

        List<IncidentRecord> manufacturerHistory = incidentService
                .getManufacturerIncidentHistory(request.getManufacturerId())
                .stream()
                .map(Incident::toIncidentRecord)
                .toList();
        List<IncidentRecord> operatorHistory = incidentService
                .getOperatorIncidentHistory(request.getOperatorId())
                .stream()
                .map(Incident::toIncidentRecord)
                .toList();

        KbmRequest kbmRequest = new KbmRequest(
                UUID.randomUUID().toString(),
                request.getOrderId(),
                request.getManufacturerId(),
                request.getOperatorId(),
                request.getDroneId(),
                manufacturerKbm,
                operatorKbm,
                manufacturerHistory,
                operatorHistory
        );

        incidentsInProgress.put(correlationId, incident.getIncidentId());
        return List.of(
                new AnalyticsMessage(
                        UUID.randomUUID().toString(),
                        AnalyticsMessage.AnalyticsAction.INCIDENT,
                        "insurance-service",
                        "component.analytics.response",
                        correlationId,
                        System.currentTimeMillis(),
                        incidentRequest,
                        "request"
                ),
                new AnalyticsMessage(
                        UUID.randomUUID().toString(),
                        AnalyticsMessage.AnalyticsAction.KBM_UPDATE,
                        "insurance-service",
                        "component.analytics.response",
                        correlationId,
                        System.currentTimeMillis(),
                        kbmRequest,
                        "request"
                )
        );

        // Обработка инцидента
        //Incident processedIncident = incidentService.processIncident(incident);

        // Пересчёт КБМ (ОФ5)
        /*KbmCalculation manufacturerKbm = kbmService.recalculateKbm(
                request.getManufacturerId(), "MANUFACTURER", processedIncident);
        KbmCalculation operatorKbm = kbmService.recalculateKbm(
                request.getOperatorId(), "OPERATOR", processedIncident);*/

        /*return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                *//*.status(InsuranceResponse.ResponseStatus.SUCCESS)*//*
                .coverageAmount(processedIncident.getDamageAmount())
                .paymentAmount(processedIncident.getDamageAmount()) // Заглушка
                .newManufacturerKbm(manufacturerKbm.getNewKbm())
                .newOperatorKbm(operatorKbm.getNewKbm())
                .message("Инцидент обработан, произведена выплата")
                .build();*/
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

    private InsuranceResponse processAnalyticsCalc(CalcResponse request, String correlationId) {
        if (correlationPurePurchaseType.containsKey(correlationId)){
            // возвращаем респонс для покупки а не простого рассчета
            Policy.PolicyType policyType = correlationPurePurchaseType.get(correlationId);
            correlationPurePurchaseType.remove(correlationId);

            BigDecimal droneKbm = kbmService.getDroneKbm(request.getDroneId());
            Policy policy = policyService.createPolicy(
                    request,
                    policyType,
                    Policy.PolicyStatus.active,
                    droneKbm
            );

            return InsuranceResponse.builder()
                    .responseId(UUID.randomUUID().toString())
                    .orderId(request.getOrderId())
                    .policyId(policy.getId())
                    .policyType(policyType)
                    .policyStatus(policy.getStatus())
                    .droneId(policy.getDroneId())
                    .droneKbm(policy.getDroneKbm())
                    .policyStartDate(policy.getStartDate())
                    .policyEndDate(policy.getEndDate())
                    .calculatedCost(policy.getCost())
                    .coverageAmount(request.getCoverageAmount())
                    .message("Полис успешно оформлен")
                    .build();
        }

        policyService.createPolicy(
                request,
                Policy.PolicyType.annual,
                Policy.PolicyStatus.calculated,
                kbmService.getDroneKbm(request.getDroneId())
        );

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

    private InsuranceResponse processAnalyticsIncidentResponse(IncidentResponse incidentResponse, String correlationId) {
        log.info("IN METHOD processAnalyticsIncidentResponse");
        String message = incidentResponse.isFraud()? "Suspected fraud, payment rejected" : "Инцидент обработан, произведена выплата";

        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .orderId(incidentResponse.getOrderId())
                .droneId(incidentResponse.getDroneId())
                .coverageAmount(incidentResponse.getCoverageAmount())
                .message(message)
                .newManufacturerKbm(null)
                .newOperatorKbm(null)
                .paymentAmount(incidentResponse.getPaymentAmount())
                .build();
    }

    private InsuranceResponse processAnalyticsKbmUpdate(KbmResponse kbmResponse, InsuranceResponse incompleted, String incidentId) {
        BigDecimal newManufacturerKbm = kbmResponse.getNewManufacturerKbm();
        BigDecimal newOperatorKbm = kbmResponse.getNewOperatorKbm();

        incompleted.setNewManufacturerKbm(newManufacturerKbm);
        incompleted.setNewOperatorKbm(newOperatorKbm);

        int manufacturerIncCount = incidentService.getManufacturerIncidentHistory(kbmResponse.getManufacturerId()).size();
        int operatorIncCount = incidentService.getOperatorIncidentHistory(kbmResponse.getOperatorId()).size();

        kbmService.recalculateKbm(
                kbmResponse.getManufacturerId(),
                "MANUFACTURER",
                newManufacturerKbm,
                incidentId,
                manufacturerIncCount
        );
        kbmService.recalculateKbm(
                kbmResponse.getOperatorId(),
                "OPERATOR",
                newOperatorKbm,
                incidentId,
                operatorIncCount
        );

        return incompleted;
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
