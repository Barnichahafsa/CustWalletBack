package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class MarkTokenUsedRequest {
    private String tokenId;
    private String customerWallet;
    private String transactionRef;

}
