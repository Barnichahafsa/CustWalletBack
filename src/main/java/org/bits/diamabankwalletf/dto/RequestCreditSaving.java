package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestCreditSaving {

    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String amount;
    private String currency;
    private String reason;
    private String requestId;
    private String requestDate;
    private String entityId;

}
