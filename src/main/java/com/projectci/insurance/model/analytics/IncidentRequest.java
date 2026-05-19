package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("manufacturer_id")
    private String manufacturerId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("drone_id")
    private String droneId;

    @JsonProperty("coverage_amount")
    private BigDecimal coverageAmount;

    @JsonProperty("incident")
    private AnalyticsIncident incident;
}
