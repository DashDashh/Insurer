package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMessage {
    @JsonProperty("message_id")
    private String messageId;      // Уникальный ID сообщения
    @JsonProperty("action")
    private AnalyticsAction action;          // Тип действия (из GatewayActions)
    @JsonProperty("sender")
    private String sender;          // ID отправителя
    @JsonProperty("reply_to")
    private String replyTo;
    @JsonProperty("correlation_id")
    private String correlationId;   // Для request-response
    @JsonProperty("timestamp")
    private Long timestamp;         // Временная метка

    // Полезная нагрузка
    @JsonProperty("payload")
    private Object payload;

    // Метаданные
    @JsonProperty("message_type")
    private String messageType;

    public enum AnalyticsAction {
        CALCULATION,
        INCIDENT,
        KBM_UPDATE
    }
}
