package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestBalanceEnquiry {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String pin;
    private String bank;
    private String requestId;
    private String requestDate;
    private String entityId;
}
