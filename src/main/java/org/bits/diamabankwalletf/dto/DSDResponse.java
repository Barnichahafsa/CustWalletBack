package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DSDResponse {
    private String status;
    private String respCode;
    private String message;
    private String authCode;
    private String requestId;
    private String result;
}
