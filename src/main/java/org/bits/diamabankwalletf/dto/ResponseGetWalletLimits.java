package org.bits.diamabankwalletf.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResponseGetWalletLimits {
    private String status;
    private String respCode;
    private String message;
    private LimitSummary summary;
    private List<LimitDetail> limits;
}
