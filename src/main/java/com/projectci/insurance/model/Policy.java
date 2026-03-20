package com.projectci.insurance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Data
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("id")
    private String id;

    @Column(unique = true)
    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("manufacturer_id")
    private String manufacturerId;
    @JsonProperty("operator_id")
    private String operatorId;
    @JsonProperty("drone_id")
    private String droneId;

    @JsonProperty("start_date")
    private LocalDateTime startDate;
    @JsonProperty("end_date")
    private LocalDateTime endDate;

    @JsonProperty("cost")
    private BigDecimal cost;
    @JsonProperty("coverage_amount")
    private BigDecimal coverageAmount;

    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    private PolicyStatus status;

    public enum PolicyStatus {
        ACTIVE, TERMINATED, EXPIRED, PAID
    }
}