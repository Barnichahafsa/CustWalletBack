package org.bits.diamabankwalletf.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeysResponse {
    private boolean success;
    private String message;
    private String bankCode;
    private String secretKey;
    private String iv;
    private Integer keyVersion;
}
