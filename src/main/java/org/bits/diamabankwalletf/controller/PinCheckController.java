package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestCheckPin;
import org.bits.diamabankwalletf.dto.ResponseServiceJson;
import org.bits.diamabankwalletf.service.PinCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class PinCheckController {

    private final PinCheckService pinCheckService;

    @PostMapping("/check-pin")
    public ResponseEntity<ResponseServiceJson> checkPin(@RequestBody RequestCheckPin request) {
        log.info("Received PIN check request for walletNumber={}, source={}",
                request.getWalletNumber(), request.getSource());

        ResponseServiceJson response = pinCheckService.checkPin(request);

        log.info("Returning PIN check response: code={}, message={}",
                response.getRespCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
