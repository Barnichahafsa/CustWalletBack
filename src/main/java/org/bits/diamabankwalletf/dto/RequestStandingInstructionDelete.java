package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestStandingInstructionDelete {
    private String phoneNumber;
    private String id;
    @JsonIgnore
    private String destBank;
    @JsonIgnore
    private String bank;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
}
