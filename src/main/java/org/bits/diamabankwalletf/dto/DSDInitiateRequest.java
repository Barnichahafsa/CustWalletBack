package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class DSDInitiateRequest {
    private String phoneNumber;
    private String chassisNumber;
    private String category;
    private String amount;
    private String bank;
    private String requestId;
    private String requestDate;
    private String key;
    private String entityId;
    private String provider;
    private String destination;
    private String currency;
}
