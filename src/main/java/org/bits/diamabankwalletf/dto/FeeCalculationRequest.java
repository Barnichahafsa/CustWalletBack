package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class FeeCalculationRequest {
    private String processingCode;      // e.g., "41" for wallet-to-wallet
    private String bankCode;            // e.g., "00100"
    private String actionCode;          // "DEB" or "CRD"
    private String currencyCode;        // e.g., "324"
    private Double transactionAmount;   // e.g., 120000
    private String walletProductCode;   // e.g., "001"
    private String walletType;          // e.g., "P" or "B"
    private String origin;              // e.g., "W"
    private String messageType;         // Optional, auto-detected if null
    private String requestId;           // Optional, generated if not provided
    private String requestDate;         // Optional, generated if not provided
}
