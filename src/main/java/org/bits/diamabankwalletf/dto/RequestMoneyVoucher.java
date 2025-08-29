package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestMoneyVoucher {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    @JsonIgnore
    private String bank;
    private String currency;
    private String amount;
    private String receiverPhone;
    private String reason;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
    private String sponsorWallet;
}
