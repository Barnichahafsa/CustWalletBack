package org.bits.diamabankwalletf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestListBanks {

    private String requestId;
    private String requestDate;
    private String entityId;

}
