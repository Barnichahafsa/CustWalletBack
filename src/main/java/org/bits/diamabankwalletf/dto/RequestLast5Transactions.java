package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestLast5Transactions {
    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String dateFrom;
    private String dateTo;
    private String pin;
    @JsonIgnore
    private String bank;
    private String requestId;
    private String requestDate;
    private String entityId;
    private String type;
}
