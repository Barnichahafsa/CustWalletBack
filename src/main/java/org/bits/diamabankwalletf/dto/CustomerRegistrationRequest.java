package org.bits.diamabankwalletf.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerRegistrationRequest {
    // Customer details
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String birthDate;
    private String nationality;
    private String documentCode;
    private String documentId;
    private String gender;

    // Wallet details
    private String currencyCode;
    private String bank;
    private String branchCode;
    private String secretQ;
    private String answer;
    private String address;
    private String type;
    private String productCode;

    // Other details
    private String entityId;
    private String lastActionUser;
    private String pin;
    private String nkinName;
    private String nkinPhone;
    private String srcFunds;
    private String occupation;
    private String estimatedIncome;
    private String referralCode;

    // System fields
    private String deviceId;
    private boolean isFirstLogin;

    // Biometric data
    private String faceId;
    private String documentImg;
    private String documentImgBack;
}
