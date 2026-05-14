package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {
    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("damage_amount")
    private BigDecimal damageAmount;
}
