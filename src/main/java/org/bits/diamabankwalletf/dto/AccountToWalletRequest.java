package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountToWalletRequest {
    private String walletNumber;
    private String phoneNumber;
    private String source; // "P" for phone number, "W" for wallet number
    private String desBank;
    private String srcBank;
    private String pin;
    private String srcAccount;
    private String accountHolder;
    private String amount;
    private String currency;
    private String reason;
    private String otp;
    private String requestId;
    private String requestDate;
    private String entityId;
}
