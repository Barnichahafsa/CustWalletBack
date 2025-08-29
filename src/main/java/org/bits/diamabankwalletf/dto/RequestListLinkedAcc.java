package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestListLinkedAcc {

    private String walletNumber;
    private String phoneNumber;
    private String source;
    private String bank;
    private String requestId;
    private String requestDate;
    private String entityId;

    @Override
    public String toString() {
        return "RequestListLinkedAcc [walletNumber=" + walletNumber + ", phoneNumber=" + phoneNumber + ", source="
                + source + ", bank=" + bank + ", requestId=" + requestId + ", requestDate=" + requestDate
                + ", entityId=" + entityId + "]";
    }
}
