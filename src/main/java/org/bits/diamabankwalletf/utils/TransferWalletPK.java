package org.bits.diamabankwalletf.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferWalletPK implements Serializable {
    private String internalReferenceNumber;
    private String bankCode;
    private String walletNumber;
    private String signOperation;
    private Integer sequenceNumber;
}
