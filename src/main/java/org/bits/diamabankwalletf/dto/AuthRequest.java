package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String phoneNumber;
    private String pin;
    private String deviceId;
}
