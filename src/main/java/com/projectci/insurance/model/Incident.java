package com.projectci.insurance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.projectci.insurance.model.analytics.IncidentRecord;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("id")
    private String id;

    @JsonProperty("incident_id")
    private String incidentId;
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("policy_id")
    private String policyId;
    @JsonProperty("manufacturer_id")
    String manufacturerId;
    @JsonProperty("operator_id")
    String operatorId;

    @JsonProperty("damage_amount")
    private BigDecimal damageAmount;
    /*@JsonProperty("incident_date")
    private LocalDateTime incidentDate;*/

    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    private IncidentStatus status;

    public enum IncidentStatus {
        REPORTED, PROCESSED, PAID, REJECTED
    }

    public IncidentRecord toIncidentRecord() {
        return new IncidentRecord(this.damageAmount);
    }
}
