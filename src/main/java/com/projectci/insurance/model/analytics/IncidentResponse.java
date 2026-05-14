package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {
    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;

    @JsonProperty("remaining_coverage")
    private BigDecimal remainingCoverage;
}
