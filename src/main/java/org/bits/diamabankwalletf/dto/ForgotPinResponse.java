package org.bits.diamabankwalletf.dto;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPinResponse {
    private boolean success;
    private String message;
    private String respCode;
    private String sessionToken;
    private String verificationToken;
    private String resetToken;
    private Object question; // Single security question
    private int remainingAttempts;
    private long retryAfter; // milliseconds
    private String authCode;
}
