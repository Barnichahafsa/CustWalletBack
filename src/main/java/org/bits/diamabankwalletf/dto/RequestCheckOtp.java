package org.bits.diamabankwalletf.dto;

import lombok.*;

import lombok.Data;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestCheckOtp {
    private String phoneNumber;
    private String authCode;
    private String otp;
    private String requestId;
    private String requestDate;
    private String entityId;
}
