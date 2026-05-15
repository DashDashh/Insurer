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
public class KbmResponse {
    @JsonProperty("request_id")
    private String requestId;

    @NotBlank
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("manufacturer_id")
    private String manufacturerId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("drone_id")
    private String droneId;

    @JsonProperty("old_manufacturer_kbm")
    private BigDecimal oldManufacturerKbm;

    @JsonProperty("old_operator_kbm")
    private BigDecimal oldOperatorKbm;

    @JsonProperty("new_manufacturer_kbm")
    private BigDecimal newManufacturerKbm;

    @JsonProperty("new_operator_kbm")
    private BigDecimal newOperatorKbm;

    @JsonProperty("manufacturer_incidents_count")
    private Integer manufacturerIncidentsCount;

    @JsonProperty("operator_incidents_count")
    private Integer operatorIncidentsCount;
}
