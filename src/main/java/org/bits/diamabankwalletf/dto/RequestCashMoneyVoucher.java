package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestCashMoneyVoucher {
    private String phoneNumber;
    private String token;
    private String pin;
    @JsonIgnore
    private String bank;
    private String requestId;
    private String requestDate;
    private String entityId;

}
