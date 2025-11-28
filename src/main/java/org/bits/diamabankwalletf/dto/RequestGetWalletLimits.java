package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class RequestGetWalletLimits {
    private String walletNumber;
    private String phoneNumber;
    private String bank;
}
