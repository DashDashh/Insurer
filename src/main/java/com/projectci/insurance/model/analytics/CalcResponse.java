package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcResponse {
    @JsonProperty("calculated_cost")
    private BigDecimal calculatedCost;

    @JsonProperty("risk_score")
    private BigDecimal riskScore;
}
