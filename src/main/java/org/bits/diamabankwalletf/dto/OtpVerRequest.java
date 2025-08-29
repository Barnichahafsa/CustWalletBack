package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class OtpVerRequest {
    private String phoneNumber;
    private String authCode;
    private String otp;
    private String entityId;
}
