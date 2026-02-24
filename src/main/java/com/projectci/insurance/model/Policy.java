package com.projectci.insurance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Data
@Getter
@Setter
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String policyNumber;

    private String orderId;
    private String manufacturerId;
    private String operatorId;
    private String droneId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private BigDecimal cost;
    private BigDecimal coverageAmount;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    public enum PolicyStatus {
        ACTIVE, TERMINATED, EXPIRED, PAID
    }
}