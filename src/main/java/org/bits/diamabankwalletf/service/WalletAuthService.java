package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAuthService {

    private final WalletRepository walletRepository;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final PinExpiryService pinExpiryService;

    public boolean verifyPin(Wallet wallet, String plainPin) {
        String storedPin = wallet.getWalletPin();

        if (storedPin == null || storedPin.isEmpty()) {
            log.warn("Wallet PIN is empty for wallet: {}", wallet.getWalletNumber());
            return false;
        }

        try {
            // Extract the first 4 characters of the client code to use as salt
            String salt = wallet.getClientCode().substring(0, 4);

            // Hash the provided plaintext PIN using the same method used during creation
            String hashedProvidedPin = pinEncryptionUtil.hashPin(plainPin, salt);

            log.debug("Comparing PINs for wallet: {}", wallet.getWalletNumber());
            log.debug("Salt (first 4 chars of client code): {}", salt);
            log.debug("Hashed provided PIN: {}", hashedProvidedPin);
            log.debug("Stored PIN: {}", storedPin);

            // Compare the hashed input PIN with the stored hashed PIN
            return storedPin.equals(hashedProvidedPin);
        } catch (Exception e) {
            log.error("Error verifying PIN for wallet: {}", wallet.getWalletNumber(), e);
            return false;
        }
    }

    public boolean updatePin(Wallet wallet, String pin) {
        try {
            String encryptedPin = pinEncryptionUtil.encryptPin(pin, wallet.getBankCode());
            wallet.setWalletPin(encryptedPin);

            pinExpiryService.initializePinExpiry(wallet);

            walletRepository.save(wallet);

            log.info("PIN updated successfully for wallet: {}", wallet.getWalletNumber());
            return true;
        } catch (Exception e) {
            log.error("Error updating PIN for wallet: {}", wallet.getWalletNumber(), e);
            return false;
        }
    }

    public boolean handleFailedLogin(Wallet wallet) {
        int currentTries = wallet.getNumberTried() != null ? wallet.getNumberTried() : 0;
        int allowedTries = 3;

        currentTries++;
        wallet.setNumberTried(currentTries);

        boolean shouldBlock = currentTries >= allowedTries;
        if (shouldBlock) {
            wallet.setBlockAction('Y');
            wallet.setBlockDate(new Date());
            log.warn("Blocking wallet due to too many failed attempts: {}", wallet.getWalletNumber());
        }

        walletRepository.save(wallet);
        return shouldBlock;
    }


    public void resetFailedAttempts(Wallet wallet) {
        if (wallet.getNumberTried() != null && wallet.getNumberTried() > 0) {
            wallet.setNumberTried(0);
            walletRepository.save(wallet);
            log.info("Reset failed attempts for wallet: {}", wallet.getWalletNumber());
        }
    }
}
