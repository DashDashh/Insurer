package com.projectci.insurance.model.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMessage {
    @JsonProperty("action")
    private AnalyticsAction action;
    @JsonProperty("sender")
    private String sender;
    @JsonProperty("payload")
    private Object payload;

    public enum AnalyticsAction {
        CALCULATION,
        CALCULATION_RESULT,
        INCIDENT,
        INCIDENT_RESULT,
        KBM_UPDATE,
        KBM_UPDATE_RESULT
    }
}
