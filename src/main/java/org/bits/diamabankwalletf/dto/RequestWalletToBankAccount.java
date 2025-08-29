package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestWalletToBankAccount {

    private String walletNumber;
    private String phoneNumber;
    private String source;

    @JsonIgnore
    private String srcBank;
    private String desAccount;
    private String bankDes;
    private String amount;
    private String currency;
    private String reason;
    private String pin;
    private String authCode;
    private String requestId;
    private String requestDate;
    private String entityId;
    private String sponsorWallet;
}
