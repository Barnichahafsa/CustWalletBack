package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestWalletToMB {

    private String walletNumber;          // Source wallet number
    private String phoneNumber;           // Wallet phone number
    private String source;                // Identifier used (P = Phone Number, W : Wallet Number)

    @JsonIgnore
    private String bank;                  // Bank code

    private String amount;                // Amount of the transaction
    private String currency;              // Currency code of the transaction
    private String provider;              // Code of the mobile money provider
    private String reason;                // Transfer reason
    private String authCode;              // Session id received during NE
    private String receiverPhone;         // Phone number of the receiver
    private String pin;                   // Hex value of encrypted pin code
    private String requestId;             // Id for each request generated from current date plus 6 random numbers(yymmddxxxxxx)
    private String requestDate;           // Timestamp when the request was sent format : yyyy-mm-dd hh:mm:ss
    private String entityId;              // Id to identify the agent/Merchant/branch performing the transaction
    private String sponsorWallet;         // Wallet Number of Sponsor Wallet (optional)
}
