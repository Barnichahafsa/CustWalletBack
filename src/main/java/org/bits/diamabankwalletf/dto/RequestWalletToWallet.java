package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestWalletToWallet {
    private String srcWalletNumber;
    private String srcPhoneNumber;
    private String source;
    private String srcBank;
    private String amount;
    private String currency;
    private String desWalletNumber;
    private String desPhoneNumber;
    private String desBank;
    private String reason;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
    private String sponsorWallet;
}
