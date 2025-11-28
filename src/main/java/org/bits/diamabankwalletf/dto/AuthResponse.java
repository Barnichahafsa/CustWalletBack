package org.bits.diamabankwalletf.dto;

import lombok.*;
import net.sf.json.JSONObject;

import java.util.List;

@RequiredArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class  AuthResponse {
    // Basic response fields
    private boolean success;
    private String token;
    private String message;
    private String respCode;

    // User information
    private String userCode;
    private String phoneNumber;
    private String name;
    private String email;
    private String type;  // "Customer"
    private String userType;  // "C" or "G"

    // Wallet information
    private String walletnumber;

    // Bank information
    private String bankCode;
    private String bankWording;
    private String branchCode;

    // Session information
    private long tokenTimeout;

    // Device information
    private String deviceIdentifier;

    // Lists and collections
    private List<?> notificationList;
    private List<JSONObject> QuestionsList;
    private List<?> NationalitiesList;
    private List<?> providerList;
    private List<?> airtimeProviderList;
    private List<?> reasonList;
    private List<?> branchList;
    private List<?> processingCodes;

    // Advertisements
    private List<?> ads;
    private int adsDelay;

    private boolean requiresPinChange = false;


    // Constructors for simpler responses
    public AuthResponse(boolean success, String token, String message) {
        this.success = success;
        this.token = token;
        this.message = message;
    }

    public AuthResponse(boolean success, String token, String message, String respCode) {
        this.success = success;
        this.token = token;
        this.message = message;
        this.respCode = respCode;
    }

    public AuthResponse(boolean success, String token, String message, String respCode, String userType) {
        this.success = success;
        this.token = token;
        this.message = message;
        this.respCode = respCode;
        this.userType = userType;
    }
}
