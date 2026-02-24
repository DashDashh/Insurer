package com.projectci.insurance.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kbm_calculations")
@Data
public class KbmCalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String entityId; // manufacturerId или operatorId
    private String entityType; // MANUFACTURER, OPERATOR

    private BigDecimal currentKbm;
    private BigDecimal newKbm;

    private int incidentCount;
    private LocalDateTime calculationDate;

    private String relatedIncidentId;
}
