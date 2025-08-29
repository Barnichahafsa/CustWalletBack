package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Advertisement;
import org.bits.diamabankwalletf.repository.AdvertisementRepository;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    public List<Map<String, Object>> getAdsForBank(String bankCode) {
        try {
            log.info("Getting advertisements for bankCode=[{}]", bankCode);

            List<Advertisement> ads = advertisementRepository.findByBankCode(bankCode);

            return ads.stream()
                    .map(ad -> {
                        Map<String, Object> adMap = new HashMap<>();
                        adMap.put("id", ad.getId());
                        adMap.put("description", ad.getDescription());
                        adMap.put("date", ad.getLastUpdate().getTime());

                        if (ad.getImage() != null) {
                            String base64Image = Base64.getEncoder().encodeToString(ad.getImage());
                            adMap.put("imageBase64", base64Image);
                        } else {
                            adMap.put("imageBase64", "");
                        }

                        return adMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving advertisements: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

        public Advertisement createAdvertisement(String bankCode, String description, String imageBase64) {
            try {
                log.info("Creating new advertisement for bankCode=[{}]", bankCode);

                if (bankCode == null || bankCode.isEmpty()) {
                    throw new IllegalArgumentException("Bank code is required");
                }
                if (description == null || description.isEmpty()) {
                    throw new IllegalArgumentException("Description is required");
                }

                Advertisement newAd = new Advertisement();
                newAd.setId(generateId());
                newAd.setBankCode(bankCode);
                newAd.setDescription(description);
                newAd.setLastUpdate(new Date());

                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                        newAd.setImage(imageBytes);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid Base64 image data: {}", e.getMessage());
                        throw new IllegalArgumentException("Invalid image data format");
                    }
                }

                // Save the advertisement to the database
                Advertisement savedAd = advertisementRepository.save(newAd);
                log.info("Advertisement created with ID: {}", savedAd.getId());

                return savedAd;
            } catch (Exception e) {
                log.error("Error creating advertisement: {}", e.getMessage());
                throw e;
            }
        }

        private String generateId() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String randomDigits = String.format("%06d", new Random().nextInt(1000000));
            return timestamp ;
        }
    }


