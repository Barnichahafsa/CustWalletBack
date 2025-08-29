package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseService {
    private String status;
    private String authCode;
    private String respCode;
    private String message;
    private String requestId;
    private String result;
}
