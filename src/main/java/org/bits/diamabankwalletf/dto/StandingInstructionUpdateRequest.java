package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StandingInstructionUpdateRequest  {
    private String phoneNumber;
    private String id;
    @JsonIgnore
    private String bank;
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
