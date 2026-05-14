package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KbmResponse {
    @JsonProperty("new_manufacturer_kbm")
    private BigDecimal newManufacturerKbm;

    @JsonProperty("new_operator_kbm")
    private BigDecimal newOperatorKbm;
}
