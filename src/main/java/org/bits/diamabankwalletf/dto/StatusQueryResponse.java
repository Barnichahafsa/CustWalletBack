package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusQueryResponse {
    private String code;
    private String message;
    private StatusQueryData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusQueryData {
        private String status;
        private String numeroTelephone;
        private String categVehicule;
        private String numeroChassis;
        private String amount;
        private String result;

        // Optional: additional details from backend
        private String operationId;
        private String transactionId;
        private String motif;
        private String createdAt;
        private String validatedAt;
    }
}
