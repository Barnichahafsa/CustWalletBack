package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.AuthResponse;
import org.bits.diamabankwalletf.model.EpsProfile;
import org.bits.diamabankwalletf.repository.EpsProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EpsProfileRepository epsProfileRepository;


    public ResponseEntity<AuthResponse> checkAppVersion(String userAgent) {
        // Validate user agent
        if (userAgent == null || userAgent.isEmpty()) {
            return null;
        }

        log.info("Checking app version for userAgent=[{}]", userAgent);

        // Extract device type
        boolean isAndroid = userAgent.contains("OS=[Android]");
        boolean isIOS = userAgent.contains("OS=[iOS]");

        if (!isAndroid && !isIOS) {
            return null; // Not a supported mobile device
        }

        // Get version configs
        String configCode = isAndroid ? "ANDROID_LATEST_VER_CUSTOMER" : "IOS_LATEST_VER_CUSTOMER";
        String mandatoryCode = isAndroid ? "ANDROID_MANDATORY_UPD_CUSTOMER" : "IOS_MANDATORY_UPD_CUSTOMER";

        // Get latest version and mandatory update flag with null checks
        Optional<EpsProfile> versionProfile = epsProfileRepository.findByCode(configCode);
        Optional<EpsProfile> mandatoryProfile = epsProfileRepository.findByCode(mandatoryCode);

        if (versionProfile.isEmpty() || mandatoryProfile.isEmpty()) {
            log.warn("Version configuration not found for {}", configCode);
            return null;
        }

        String latestVersion = versionProfile.get().getValue();
        boolean mandatoryUpdate = "Y".equals(mandatoryProfile.get().getValue());

        if (!mandatoryUpdate) {
            return null; // Update not mandatory
        }

        // Extract client version from user agent
        String clientVersion = extractVersionFromUserAgent(userAgent);
        if (clientVersion == null) {
            log.warn("Could not extract version from user agent");
            return null;
        }

        log.info("********************** start : app version control *******************************");
        log.info("ReceivedVersion: {}", clientVersion);
        log.info("DBVersion: {}", latestVersion);

        // Compare versions and return response if update needed
        if (isUpdateRequired(clientVersion, latestVersion)) {
            log.info("Check app version - App Version Outdated");
            return ResponseEntity.status(403)
                    .body(new AuthResponse(false, null, "Kindly update, this app version is out of date."));
        }

        return null;
    }


    private String extractVersionFromUserAgent(String userAgent) {
        try {
            int startIndex = userAgent.indexOf("VERSION_NAME=[") + 14;
            int endIndex = userAgent.indexOf("] VERSION_CODE=[", startIndex);

            if (startIndex >= 14 && endIndex > startIndex) {
                return userAgent.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.error("Error extracting version from user agent: {}", e.getMessage());
        }
        return null;
    }


    private boolean isUpdateRequired(String clientVersion, String requiredVersion) {
        try {
            String[] clientParts = clientVersion.split("\\.");
            String[] requiredParts = requiredVersion.split("\\.");

            // Ensure we have at least 3 parts for comparison
            if (clientParts.length < 3 || requiredParts.length < 3) {
                return false;
            }

            // Compare major version
            int clientMajor = Integer.parseInt(clientParts[0]);
            int requiredMajor = Integer.parseInt(requiredParts[0]);

            if (clientMajor < requiredMajor) {
                return true;
            }
            if (clientMajor > requiredMajor) {
                return false;
            }

            // Major versions equal, compare minor version
            int clientMinor = Integer.parseInt(clientParts[1]);
            int requiredMinor = Integer.parseInt(requiredParts[1]);

            if (clientMinor < requiredMinor) {
                return true;
            }
            if (clientMinor > requiredMinor) {
                return false;
            }

            // Minor versions equal, compare patch version
            int clientPatch = Integer.parseInt(clientParts[2]);
            int requiredPatch = Integer.parseInt(requiredParts[2]);

            return clientPatch < requiredPatch;
        } catch (NumberFormatException e) {
            log.error("Error parsing version numbers: {}", e.getMessage());
            return false;
        }
    }
}
