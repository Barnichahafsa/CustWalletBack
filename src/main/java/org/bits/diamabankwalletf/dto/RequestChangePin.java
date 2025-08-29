package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestChangePin {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String oldPinCode;
    private String newPinCode;
    private String confPinCode;
    private String requestId;
    private String requestDate;
    private String entityId;

}
