package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestApproval {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String action;
    private String pin;
    private String transactionRef;
    private String transactionType;
    private String requestId;
    private String requestDate;
    private String entityId;
}
