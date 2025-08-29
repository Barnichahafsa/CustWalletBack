package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestAgenteStatement {
    @JsonIgnore
    private String bank;
    private String type;
    private String dateFrom;
    private String dateTo;
    private String requestId;
    private String requestDate;
    private String entityId;
}
