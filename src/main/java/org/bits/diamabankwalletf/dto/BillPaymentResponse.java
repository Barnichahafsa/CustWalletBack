package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillPaymentResponse {
    private String code;
    private String message;
    private BillPaymentData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BillPaymentData {
        private String status;
        private String numeroTelephone;
        private String categVehicule;
        private String numeroChassis;
        private String amount;
        private String result;
    }
}
