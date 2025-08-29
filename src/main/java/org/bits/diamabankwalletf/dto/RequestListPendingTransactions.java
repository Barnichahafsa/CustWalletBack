package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestListPendingTransactions {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    @JsonIgnore
    private String bank;
    private String pin;
    private String requestId;
    private String requestDate;
    private String entityId;
}
