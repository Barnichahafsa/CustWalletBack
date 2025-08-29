package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestTopUpPurchase {
    private String walletNumber;
    private String lineType;
    private String productId;
    private String phoneNumber;
    private String source;
    private String srcBank;
    private String recipient;
    private String amount;
    private String currency;
    private String type;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
    private String sponsorWallet;
}
