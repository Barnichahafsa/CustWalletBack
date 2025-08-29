package org.bits.diamabankwalletf.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDataPK implements Serializable {
    private String walletNumber;
    private String supplementaryData1;
    private String bankCode;
}
