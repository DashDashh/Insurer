package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcResponse {
    @NotBlank
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("manufacturer_id")
    private String manufacturerId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("drone_id")
    private String droneId;

    @JsonProperty("calculated_cost")
    private BigDecimal calculatedCost;

    @JsonProperty("risk_score")
    private BigDecimal riskScore;

    @JsonProperty("security_goals")
    private List<String> securityGoals;

    @JsonProperty("required_goals")
    private List<String> requiredGoals;

    @JsonProperty("coverage_amount")
    private BigDecimal coverageAmount;
}
