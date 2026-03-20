package com.projectci.insurance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InsuranceResponse {
    @JsonProperty("response_id")
    private String responseId;
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("policy_id")
    private String policyId;
    @JsonProperty("status")
    private ResponseStatus status;
    @JsonProperty("message")
    private String message;

    // Для расчёта
    @JsonProperty("calculated_cost")
    private BigDecimal calculatedCost;
    @JsonProperty("manufacturer_kbm")
    private BigDecimal manufacturerKbm;
    @JsonProperty("operator_kbm")
    private BigDecimal operatorKbm;

    // Для полиса
    @JsonProperty("policy_start_date")
    private LocalDateTime policyStartDate;
    @JsonProperty("policy_end_date")
    private LocalDateTime policyEndDate;

    // Для инцидентов
    @JsonProperty("coverage_amount")
    private BigDecimal coverageAmount; // для расчёта и покупки тоже
    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;
    @JsonProperty("new_manufacturer_kbm")
    private BigDecimal newManufacturerKbm;
    @JsonProperty("new_operator_kbm")
    private BigDecimal newOperatorKbm;

    public enum ResponseStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}