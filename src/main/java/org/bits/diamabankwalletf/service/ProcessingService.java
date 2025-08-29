package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.ProcessingCode;
import org.bits.diamabankwalletf.repository.ProcessingCodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingService {

    private final ProcessingCodeRepository processingCodeRepository;

    /**
     * Get all processing codes as map objects with id and label fields
     * @return List of processing codes as maps
     */
    public List<Map<String, Object>> getProcessingCodes() {
        log.info("Getting all processing codes");
        return processingCodeRepository.findAllAsMap();
    }

    /**
     * Get a processing code by its code
     * @param code The processing code
     * @return Optional containing the processing code if found
     */
    public Optional<ProcessingCode> getProcessingCode(String code) {
        log.info("Getting processing code for code=[{}]", code);
        return Optional.ofNullable(processingCodeRepository.findByCode(code));
    }

    /**
     * Get processing code wording by its code
     * @param code The processing code
     * @return The wording or null if not found
     */
    public String getProcessingCodeWording(String code) {
        log.info("Getting processing code wording for code=[{}]", code);
        return getProcessingCode(code)
                .map(ProcessingCode::getWording)
                .orElse(null);
    }

    /**
     * Check if a processing code exists
     * @param code The processing code
     * @return true if the code exists, false otherwise
     */
    public boolean isValidProcessingCode(String code) {
        log.info("Checking if processing code is valid for code=[{}]", code);
        return processingCodeRepository.existsById(code);
    }

    /**
     * Count all processing codes
     * @return The number of processing codes
     */
    public long countProcessingCodes() {
        return processingCodeRepository.count();
    }
}
