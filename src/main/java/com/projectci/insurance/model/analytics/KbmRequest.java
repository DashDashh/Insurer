package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KbmRequest {
    @JsonProperty("manufacturer_kbm")
    private BigDecimal manufacturerKbm;

    @JsonProperty("operator_kbm")
    private BigDecimal operatorKbm;

    @JsonProperty("manufacturer_history")
    private List<IncidentRecord> manufacturerHistory;

    @JsonProperty("operator_history")
    private List<IncidentRecord> operatorHistory;

}
