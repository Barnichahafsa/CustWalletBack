package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.json.JSONObject;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JResponseService {
    private String status;
    private String authCode;
    private String respCode;
    private String message;
    private String requestId;
    private List<JSONObject> result;
}
