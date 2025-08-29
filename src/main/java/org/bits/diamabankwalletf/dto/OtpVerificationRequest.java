package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String phoneNumber;
    private String otp;
    private String deviceId;
}
