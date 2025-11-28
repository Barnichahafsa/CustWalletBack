package org.bits.diamabankwalletf.service;


import org.bits.diamabankwalletf.dto.FeeCalculationRequest;
import org.bits.diamabankwalletf.dto.FeeCalculationResponse;
import org.bits.diamabankwalletf.model.AuthorizationFee;
import org.bits.diamabankwalletf.repository.AuthorizationFeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FeeCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(FeeCalculationService.class);

    @Autowired
    private AuthorizationFeeRepository feeRepository;

    /**
     * Calculate fee based on authorization_fees table rules
     */
    public FeeCalculationResponse calculateFee(FeeCalculationRequest request) {
        logger.debug("Calculating fee for request: {}", request);

        // Validate request
        try {
            validateRequest(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return FeeCalculationResponse.builder()
                    .respCode("003")
                    .message("Invalid request: " + e.getMessage())
                    .feeAmount(0.0)
                    .requestId(request.getRequestId())
                    .build();
        }

        BigDecimal amount = BigDecimal.valueOf(request.getTransactionAmount());

        // Find applicable fee rules
        List<AuthorizationFee> applicableFees = feeRepository.findApplicableFees(
                request.getProcessingCode(),
                request.getCurrencyCode(),
                request.getWalletProductCode(),
                request.getWalletType(),
                request.getActionCode(),
                request.getBankCode(),
                request.getOrigin(),
                request.getMessageType() != null ? request.getMessageType() : "ON_US",
                amount
        );

        if (applicableFees.isEmpty()) {
            logger.warn("No fee rule found for processingCode: {}, amount: {}",
                    request.getProcessingCode(), amount);

            return FeeCalculationResponse.builder()
                    .respCode("001")
                    .message("No applicable fee rule found")
                    .feeAmount(0.0)
                    .requestId(request.getRequestId())
                    .build();
        }

        // Use the first (most specific) rule
        AuthorizationFee feeRule = applicableFees.get(0);

        logger.info("Applying fee rule - Index: {}, Wording: {}, Rate: {}, Fixed: {}",
                feeRule.getIndexFees(),
                feeRule.getWording(),
                feeRule.getRate(),
                feeRule.getFixedAmount());

        // Calculate the fee
        BigDecimal calculatedFee = calculateFeeAmount(amount, feeRule);

        return FeeCalculationResponse.builder()
                .respCode("000")
                .message("Fee calculated successfully")
                .feeAmount(calculatedFee.doubleValue())
                .ruleIndex(String.valueOf(feeRule.getIndexFees()))
                .requestId(request.getRequestId())
                .build();
    }

    /**
     * Calculate fee amount based on rule
     * Rate is stored as percentage (0.95 = 0.95%)
     * Formula: amount * (rate / 100) OR fixed amount, whichever is greater
     */
    private BigDecimal calculateFeeAmount(BigDecimal amount, AuthorizationFee rule) {
        BigDecimal fee = BigDecimal.ZERO;

        // Calculate rate-based fee
        if (rule.getRate() != null && rule.getRate().compareTo(BigDecimal.ZERO) > 0) {
            // Rate is percentage: 0.95 means 0.95%, so divide by 100 to get decimal
            // Example: 1,000,000 * 0.95 / 100 = 9,500
            fee = amount.multiply(rule.getRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            logger.debug("Rate-based fee: {} * {} / 100 = {}", amount, rule.getRate(), fee);
        }

        // Use fixed amount if specified and greater than rate-based fee
        if (rule.getFixedAmount() != null && rule.getFixedAmount().compareTo(fee) > 0) {
            fee = rule.getFixedAmount();
            logger.debug("Using fixed amount: {}", fee);
        }

        // Round to nearest whole number for GNF (no decimals)
        fee = fee.setScale(0, RoundingMode.HALF_UP);

        logger.info("Final calculated fee: {}", fee);

        return fee;
    }

    private void validateRequest(FeeCalculationRequest request) {
        if (request.getProcessingCode() == null || request.getProcessingCode().isEmpty()) {
            throw new IllegalArgumentException("Processing code is required");
        }

        if (request.getTransactionAmount() == null || request.getTransactionAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }

        if (request.getCurrencyCode() == null || request.getCurrencyCode().isEmpty()) {
            throw new IllegalArgumentException("Currency code is required");
        }

        if (request.getWalletProductCode() == null || request.getWalletProductCode().isEmpty()) {
            throw new IllegalArgumentException("Wallet product code is required");
        }

        if (request.getWalletType() == null || request.getWalletType().isEmpty()) {
            throw new IllegalArgumentException("Wallet type is required");
        }

        if (request.getActionCode() == null || request.getActionCode().isEmpty()) {
            throw new IllegalArgumentException("Action code is required");
        }

        if (request.getBankCode() == null || request.getBankCode().isEmpty()) {
            throw new IllegalArgumentException("Bank code is required");
        }

        if (request.getOrigin() == null || request.getOrigin().isEmpty()) {
            throw new IllegalArgumentException("Origin is required");
        }
    }
}
