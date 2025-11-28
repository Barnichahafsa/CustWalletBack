package org.bits.diamabankwalletf.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestGetCustomerQR {

    private String requestId;
    private String requestDate;
    private String customerIdentifier;

}
