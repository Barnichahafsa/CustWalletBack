package org.bits.diamabankwalletf.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDebitSaving {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String currency;
    private String amount;
    private String reason;
    private String requestId;
    private String requestDate;
    private String entityId;
}
