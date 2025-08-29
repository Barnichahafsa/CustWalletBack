package org.bits.diamabankwalletf.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SponsoredWalletDto {
    private String bankCode;
    private String bankCodeSponsored;
    private String phoneNumber;
    private String phoneNumberSponsored;
    private String source;
    private String walletNumber;
    private String walletNumberSponsored;
    private String dateStart;
    private String dateEnd;
    private String dailyLimit;
    private String monthlyLimit;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
}
