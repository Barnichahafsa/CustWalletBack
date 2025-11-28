package org.bits.diamabankwalletf.dto;
import lombok.Data;

@Data
public class DSDValidateRequest {
    private String operationId;
    private String phoneNumber;
    private String pin;
    private String requestId;
}
