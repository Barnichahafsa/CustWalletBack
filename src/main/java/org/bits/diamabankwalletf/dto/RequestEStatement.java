package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestEStatement {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String campaignId;
    private String groupId;
    private String dateFrom;
    private String dateTo;
    private String type;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
}
