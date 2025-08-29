package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
    private String status;
    private String respCode;
    private String message;
    private String authCode;
    private String requestId;
    private String result;
    private String token;
}
