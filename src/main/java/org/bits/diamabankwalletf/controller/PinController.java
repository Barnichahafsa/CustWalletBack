package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestChangePin;
import org.bits.diamabankwalletf.dto.RequestResetPin;
import org.bits.diamabankwalletf.dto.RequestResetPinQ;
import org.bits.diamabankwalletf.dto.ResponseService;
import org.bits.diamabankwalletf.service.PinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pin")
@RequiredArgsConstructor
@Slf4j
public class PinController {

    private final PinService pinService;

    @PostMapping("/change")
    public ResponseEntity<ResponseService> changePin(@RequestBody RequestChangePin request) {
        log.info("[CHANGE PIN CONTROLLER] Received PIN change request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseService response = pinService.changePin(request);

        log.info("[CHANGE PIN CONTROLLER] Returning PIN change response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<ResponseService> resetPin(@RequestBody RequestResetPin request) {
        log.info("[RESET PIN CONTROLLER] Received PIN reset request for walletNumber={}",
                request.getWalletNumber());

        ResponseService response = pinService.resetPin(request);

        log.info("[RESET PIN CONTROLLER] Returning PIN reset response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resetq")
    public ResponseEntity<ResponseService> resetPinQ(@RequestBody RequestResetPinQ request) {
        log.info("[RESET PIN Q CONTROLLER] Received PIN resetQ request for walletNumber={}",
                request.getWalletNumber());

        ResponseService response = pinService.resetPinQ(request);

        log.info("[RESET PIN Q CONTROLLER] Returning PIN resetQ response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
