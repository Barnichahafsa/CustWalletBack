package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestWalletEnquiry {
    private String requestId;
    private String requestDate;
    private String source;
    private String phoneNumber;
    private String walletNumber;
    private String bank;

    private String entityId;
}
