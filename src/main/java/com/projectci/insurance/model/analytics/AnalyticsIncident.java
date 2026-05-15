package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AnalyticsIncident {
    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("damage_amount")
    private BigDecimal damageAmount;

    @JsonProperty("incident_date")
    private LocalDateTime incidentDate;

    @JsonProperty("incident_type")
    private String incidentType;
}
