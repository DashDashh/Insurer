package com.projectci.insurance.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsuranceRequest {
    private String requestId;

    @NotBlank
    private String orderId;

    private String manufacturerId;
    private String operatorId;
    private String droneId;

    // Цели безопасности
    private String droneSafetyPurpose;
    private String requiredSafetyPurpose;

    // Для расчёта (ОФ1-ОФ2)
    private String calculationId; // ID предварительного расчёта

    // Для инцидентов (ОФ4)
    private Incident incident;

    // Тип запроса: CALCULATION, PURCHASE, INCIDENT, POLICY_TERMINATION
    private RequestType requestType;

    public enum RequestType {
        CALCULATION,    // ОФ1
        PURCHASE,       // ОФ2
        INCIDENT,       // ОФ4
        POLICY_TERMINATION // ОФ3
    }
}