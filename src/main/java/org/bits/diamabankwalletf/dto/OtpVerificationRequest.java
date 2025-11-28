package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String phoneNumber;
    private String otp;

    // Map both "deviceId" and "authCode" JSON fields into this single field
    @JsonAlias({"deviceId", "authCode"})
    private String deviceId;
}
