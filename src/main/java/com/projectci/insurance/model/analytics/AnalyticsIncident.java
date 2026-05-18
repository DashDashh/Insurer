package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectci.insurance.model.Incident;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
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

    public static AnalyticsIncident fromInsuranceIncident(Incident incident) {
        AnalyticsIncident analyticsIncident = new AnalyticsIncident();
        analyticsIncident.setIncidentId(incident.getId());
        analyticsIncident.setPolicyId(incident.getPolicyId());
        analyticsIncident.setDamageAmount(incident.getDamageAmount());
        //analyticsIncident.setIncidentDate(incident.getIncidentDate());
        analyticsIncident.setIncidentType("incident");
        return analyticsIncident;
    }
}
