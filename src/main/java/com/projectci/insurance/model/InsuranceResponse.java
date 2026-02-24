package com.projectci.insurance.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InsuranceResponse {
    private String responseId;
    private String requestId;
    private String orderId;
    private String policyId;
    private ResponseStatus status;
    private String message;

    // Для расчёта
    private BigDecimal calculatedCost;
    private BigDecimal manufacturerKbm;
    private BigDecimal operatorKbm;

    // Для полиса
    private LocalDateTime policyStartDate;
    private LocalDateTime policyEndDate;

    // Для инцидентов
    private BigDecimal coverageAmount;
    private BigDecimal paymentAmount;
    private BigDecimal newManufacturerKbm;
    private BigDecimal newOperatorKbm;

    public enum ResponseStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}