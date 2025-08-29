package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.service.StandingInstructionService;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@RestController
@RequestMapping("/api/standing-instruction")
@RequiredArgsConstructor
@Slf4j
public class StandingInstructionController {

    private final StandingInstructionService standingInstructionService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @PostMapping("/list")
    public ResponseEntity<JResponseService> listStandingInstructions(@RequestBody RequestStandingInstructionList request) {
        log.info("Received standing instruction list request for phone: {}", request.getPhoneNumber());

        // Set default values if not provided
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            // Using a default entity ID for the system
            request.setEntityId("SYSTEM");
            log.debug("Set default entityId: {}", request.getEntityId());
        }

        // Call the service
        JResponseService response = standingInstructionService.listStandingInstructions(request);

        log.info("Standing instruction list completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseServiceJson> createStandingInstruction(@RequestBody StandingInstructionDTO request) {
        log.info("Received standing instruction request for phone number: {}", request.getPhoneNumber());

        // Generate requestId if not provided
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        // Generate requestDate if not provided
        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        // Call the service
        ResponseServiceJson response = standingInstructionService.createStandingInstruction(request);

        log.info("Standing instruction request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseService> updateStandingInstruction(@RequestBody StandingInstructionUpdateRequest request) {
        log.info("Received standing instruction update request for ID: {}", request.getId());

        // Generate requestId and requestDate if not provided
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        ResponseService response = standingInstructionService.updateStandingInstruction(request);

        log.info("Standing instruction update completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<ResponseService> deleteStandingInstruction(@RequestBody RequestStandingInstructionDelete request) {
        log.info("Received standing instruction deletion request for phone: {}", request.getPhoneNumber());

        // Generate requestId if not provided
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        // Generate requestDate if not provided
        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        // Set default bank code if not provided
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
        }

        // Set entityId if not provided (using a default value)
        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("000000"); // Default entity ID, adjust as needed
            log.debug("Set default entityId: {}", request.getEntityId());
        }

        request.setPin(pinEncryptionUtil.encryptPin(request.getPin(),"00100"));

        ResponseService response = standingInstructionService.deleteStandingInstruction(request);

        log.info("Standing instruction deletion completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }
}
