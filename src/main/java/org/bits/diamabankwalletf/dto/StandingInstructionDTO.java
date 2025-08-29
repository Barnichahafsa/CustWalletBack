package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StandingInstructionDTO {
    private String phoneNumber;
    private String trxType;
    private String destBank;
    private String receiver;
    private String destination;
    private String amount;
    private String currency;
    private String details;
    private String pin;
    private String frequency;
    private String startDate;
    private String endDate;
    private String requestId;
    private String requestDate;
    private String entityId;
}
