package org.bits.diamabankwalletf.controller;

import org.bits.diamabankwalletf.dto.FeeCalculationRequest;
import org.bits.diamabankwalletf.dto.FeeCalculationResponse;
import org.bits.diamabankwalletf.service.FeeCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/fees")
public class FeeCalculationController {

    private static final Logger logger = LoggerFactory.getLogger(FeeCalculationController.class);

    @Autowired
    private FeeCalculationService feeCalculationService;

    @PostMapping("/calculate")
    public ResponseEntity<FeeCalculationResponse> calculateFee( @RequestBody FeeCalculationRequest request) {
        logger.info("Fee calculation request received - processingCode: {}, amount: {}, currency: {}",
                request.getProcessingCode(),
                request.getTransactionAmount(),
                request.getCurrencyCode());

        try {
            FeeCalculationResponse response = feeCalculationService.calculateFee(request);

            logger.info("Fee calculated successfully - amount: {}, ruleIndex: {}",
                    response.getFeeAmount(),
                    response.getRuleIndex());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid fee calculation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    FeeCalculationResponse.builder()
                            .respCode("003")
                            .message("Invalid request: " + e.getMessage())
                            .build()
            );

        } catch (Exception e) {
            logger.error("Error calculating fee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    FeeCalculationResponse.builder()
                            .respCode("999")
                            .message("System error during fee calculation")
                            .build()
            );
        }
    }
}
