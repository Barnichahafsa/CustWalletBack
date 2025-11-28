package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class DSDRejectRequest {
    private String operationId;
    private String phoneNumber;
    private String requestId;
}
