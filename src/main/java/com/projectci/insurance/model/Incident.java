package com.projectci.insurance.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String incidentId;
    private String orderId;
    private String policyId;
    private String reporterId; // Агрегатор или Регулятор
    private String reporterType; // AGGREGATOR, REGULATOR

    private BigDecimal damageAmount;
    private LocalDateTime incidentDate;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    public enum IncidentStatus {
        REPORTED, PROCESSED, PAID, REJECTED
    }
}
