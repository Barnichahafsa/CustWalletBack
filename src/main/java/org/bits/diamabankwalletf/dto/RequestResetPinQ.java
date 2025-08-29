package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestResetPinQ {

    private String walletNumber;
    private String phoneNumber;
    private String bank;
    private String newPinCode;
    private String confPinCode;
    private String secretQuestion;
    private String answer;
    private String requestId;
    private String requestDate;
    private String entityId;
}
