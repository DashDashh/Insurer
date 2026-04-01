package com.projectci.insurance.service;

import com.projectci.insurance.config.TopicConfig;
import com.projectci.insurance.model.*;
import com.projectci.insurance.producer.MessagePublisher;
import com.projectci.insurance.utils.NamespaceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public InsuranceService(
            MessagePublisher messagePublisher,
            PolicyService policyService,
            IncidentService incidentService,
            KbmService kbmService,
            TopicConfig topicConfig,
            NamespaceUtils namespaceUtils,
            @Value("${spring.application.name:insurance-service}") String applicationName) {

        this.messagePublisher = messagePublisher;
        this.policyService = policyService;
        this.incidentService = incidentService;
        this.kbmService = kbmService;
        this.topicConfig = topicConfig;
        this.namespaceUtils = namespaceUtils;

        // Формируем уникальный ID отправителя с учетом namespace
        this.systemId = namespaceUtils.hasNamespace()
                ? namespaceUtils.getCurrentNamespace() + "." + applicationName
                : applicationName;
    }

    public void processInsuranceRequest(MessageRequest message) {
        log.info("Processing insurance request: {}, from system: {}",
                message.getPayload().getRequestType(), systemId);

        InsuranceRequest request = message.getPayload();
        InsuranceResponse response = null;

        try {
            // Обработка запроса
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

            // Создаем сообщение в правильном формате
            MessageResponse messageOut = MessageResponse.createResponse(
                    request.getRequestId(), // correlationId
                    response
            );

            // Определяем топик для ответа (с учетом namespace)
            // Ответ отправляем в системный топик отправителя или в компонентный топик
            String responseTopic = determineResponseTopic(message);

            // Отправка ответа
            messagePublisher.send(
                    responseTopic,
                    response.getOrderId(),
                    messageOut
            );

            log.info("Response sent to topic: {}, correlationId: {}",
                    responseTopic, messageOut.getCorrelationId());

        } catch (Exception e) {
            log.error("Error processing request: {}", request, e);

            // Создаем сообщение об ошибке
            InsuranceResponse errorResponse = createErrorResponse(request, e.getMessage());
            MessageResponse errorMessage = MessageResponse.createResponse(
                    request.getRequestId(),
                    errorResponse
            );

            // Отправляем в dead letters
            messagePublisher.send(
                    TopicConfig.DEAD_LETTERS_TOPIC,
                    errorResponse.getOrderId(),
                    errorMessage
            );
        }
    }

    /**
     * Определяет топик для ответа
     * Если отправитель указал конкретный топик - используем его
     * Иначе отправляем в системный топик отправителя
     */
    private String determineResponseTopic(MessageRequest message) {
        // ОПЦИЯ на потом
        // Если в запросе указан топик для ответа
        /*if (message.getResponseTopic() != null && !message.getResponseTopic().isEmpty()) {
            return request.getResponseTopic();
        }*/

        // Если указан отправитель - отправляем в его системный топик
        if (message.getSender() != null && !message.getSender().isEmpty()) {
            return topicConfig.getSystemTopic(message.getSender());
        }

        // По умолчанию отправляем в наш компонентный топик (для отладки)
        return topicConfig.getComponentTopic(
                TopicConfig.InsuranceSystem.Components.INSURANCE_SERVICE
        );
    }

    private InsuranceResponse processCalculation(InsuranceRequest request) {
        // ОФ1 - Заглушка для расчёта
        return InsuranceResponse.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(request.getRequestId())
                .orderId(request.getOrderId())
                .status(InsuranceResponse.ResponseStatus.SUCCESS)
                .calculatedCost(kbmService.calculatePolicyCost(request))
                .coverageAmount(request.getCoverageAmount())
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
