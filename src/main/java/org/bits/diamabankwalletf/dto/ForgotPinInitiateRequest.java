package org.bits.diamabankwalletf.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPinInitiateRequest {

    private String phoneNumber;

    private String deviceId;
}
