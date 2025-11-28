package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeCalculationResponse {
    private String respCode;
    private String message;
    private Double feeAmount;
    private String ruleIndex;
    private String requestId;
}
