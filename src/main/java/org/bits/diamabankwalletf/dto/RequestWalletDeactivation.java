package org.bits.diamabankwalletf.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestWalletDeactivation {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String pin;
    private String reason;
    private String beneficiaryName;
    private String paymentMethod;
    private String receiver;
    private String institution;
    private String requestId;
    private String requestDate;
    private String entityId;

}
