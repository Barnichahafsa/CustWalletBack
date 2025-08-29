package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseServiceJson {
    private String status;
    private String authCode;
    private String respCode;
    private String message;
    private String requestId;
    private JsonNode result; // Using Jackson's JsonNode instead of Apache Commons JSONObject
}
