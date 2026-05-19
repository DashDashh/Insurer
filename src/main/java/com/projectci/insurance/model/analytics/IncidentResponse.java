package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("manufacturer_id")
    private String manufacturerId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("drone_id")
    private String droneId;

    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;

    @JsonProperty("message")
    private String message;

    @JsonProperty("is_fraud")
    private boolean isFraud;

    @JsonProperty("coverage_amount")
    private BigDecimal coverageAmount;

    @JsonProperty("damage_amount")
    private BigDecimal damageAmount;
}
