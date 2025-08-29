package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Advertisement;
import org.bits.diamabankwalletf.service.AdvertisementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
@Slf4j
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAdvertisements(
            @RequestParam("bankCode") String bankCode) {
        log.info("GET request for advertisements with bankCode: {}", bankCode);
        List<Map<String, Object>> ads = advertisementService.getAdsForBank(bankCode);
        return ResponseEntity.ok(ads);
    }


    @PostMapping
    public ResponseEntity<?> createAdvertisement(@RequestBody CreateAdRequest request) {
        try {
            log.info("POST request to create a new advertisement for bank: {}", request.getBankCode());

            Advertisement createdAd = advertisementService.createAdvertisement(
                    request.getBankCode(),
                    request.getDescription(),
                    request.getImageBase64()
            );

            Map<String, Object> response = Map.of(
                    "id", createdAd.getId(),
                    "date", createdAd.getLastUpdate().getTime(),
                    "message", "Advertisement created successfully"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Bad request when creating advertisement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid input",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating advertisement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Server error",
                    "message", "Failed to create advertisement"
            ));
        }
    }


    public static class CreateAdRequest {
        private String bankCode;
        private String description;
        private String imageBase64;

        public String getBankCode() {
            return bankCode;
        }

        public void setBankCode(String bankCode) {
            this.bankCode = bankCode;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }
    }
}
