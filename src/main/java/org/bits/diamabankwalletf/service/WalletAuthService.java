package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Customer;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.CustomerRepository;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAuthService {

    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
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

    /**
     * Check if user is blocked - ONLY from CUSTOMER_DWS table
     */
    public boolean isUserBlocked(String phoneNumber) {
        try {
            log.info("=== CHECKING USER BLOCKED STATUS ===");
            log.info("Phone number: {}", phoneNumber);

            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);

            if (customerOpt.isEmpty()) {
                log.warn("Customer not found for block check: phoneNumber=[{}]", phoneNumber);
                log.info("Returning FALSE (customer not found)");
                return false;
            }

            Customer customer = customerOpt.get();
            String blockAccessValue = customer.getBlockAccess();

            log.info("Customer found - ID: {}", customer.getFirstName()); // or whatever ID field you have
            log.info("Block access value from DB: '{}'", blockAccessValue);
            log.info("Block access equals 'Y'? {}", "Y".equals(blockAccessValue));


            Optional<Wallet> wallet = walletRepository.findByPhoneNumber(phoneNumber);

            boolean isBlocked = "Y".equals(customer.getBlockAccess()) ||"L".equals(wallet.get().getStatusWallet());


            log.info("Final blocking decision: {}", isBlocked);
            log.info("=== END BLOCKING CHECK ===");

            return isBlocked;

        } catch (Exception e) {
            log.error("Error checking block status for phoneNumber=[{}]", phoneNumber, e);
            log.info("Returning FALSE due to exception");
            return false;
        }
    }

 public boolean isUserCancelled(String phoneNumber) {
        try {
            log.info("=== CHECKING USER BLOCKED STATUS ===");
            log.info("Phone number: {}", phoneNumber);

            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);

            if (customerOpt.isEmpty()) {
                log.warn("Customer not found for block check: phoneNumber=[{}]", phoneNumber);
                log.info("Returning FALSE (customer not found)");
                return false;
            }

            Customer customer = customerOpt.get();
            String blockAccessValue = customer.getBlockAccess();

            log.info("Customer found - ID: {}", customer.getFirstName()); // or whatever ID field you have
            log.info("Block access value from DB: '{}'", blockAccessValue);
            log.info("Block access equals 'Y'? {}", "Y".equals(blockAccessValue));


            Optional<Wallet> wallet = walletRepository.findByPhoneNumber(phoneNumber);

            boolean isCancelled = "C".equals(wallet.get().getStatusWallet()) ;


            log.info("Final cancelled decision: {}", isCancelled);
            log.info("=== END  CANCELLED CHECK ===");

            return isCancelled;

        } catch (Exception e) {
            log.error("Error checking cancel status for phoneNumber=[{}]", phoneNumber, e);
            log.info("Returning FALSE due to exception");
            return false;
        }
    }

    @Transactional
    public boolean updatePin(Wallet wallet, String pin) {
        try {
            // Update PIN in wallet table
            String encryptedPin = pinEncryptionUtil.encryptPin(pin, wallet.getBankCode());
            wallet.setWalletPin(encryptedPin);
            pinExpiryService.initializePinExpiry(wallet);
            walletRepository.save(wallet);

            // Also update PIN in customer table if customer exists
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(wallet.getPhoneNumber());
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                String customerEncryptedPin = pinEncryptionUtil.encryptPin(pin, customer.getBankCode());
                customer.setPassword(customerEncryptedPin);
                customerRepository.save(customer);
                log.info("PIN updated in both wallet and customer tables for wallet: {}", wallet.getWalletNumber());
            } else {
                log.warn("Customer not found for PIN sync, updated wallet only: {}", wallet.getWalletNumber());
            }

            log.info("PIN updated successfully for wallet: {}", wallet.getWalletNumber());
            return true;
        } catch (Exception e) {
            log.error("Error updating PIN for wallet: {}", wallet.getWalletNumber(), e);
            return false;
        }
    }

    @Transactional
    public boolean handleFailedLogin(Wallet wallet) {

        try {
            String phoneNumber = wallet.getPhoneNumber();

            // Update failed attempts in wallet table
            int currentTries = wallet.getNumberTried() != null ? wallet.getNumberTried() : 0;
            int allowedTries = 3;

            currentTries++;
            wallet.setNumberTried(currentTries);

            boolean shouldBlock = currentTries >= allowedTries;


            // Also update failed attempts in customer table if customer exists
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                int customerTries = customer.getNumberOfTries() != null ? customer.getNumberOfTries() : 0;
                customerTries++;
                customer.setNumberOfTries(customerTries);

                // Use customer's allowed tries if configured, otherwise default
                int customerAllowedTries = customer.getNumberOfTriesAllowed() != null ?
                        customer.getNumberOfTriesAllowed() : allowedTries;

                if (customerTries >= customerAllowedTries) {
                    customer.setBlockAccess("Y");
                    log.warn("Blocking customer due to too many failed attempts: phoneNumber=[{}]", phoneNumber);
                }

                customerRepository.save(customer);
                log.info("Updated failed attempts in both wallet and customer tables for phoneNumber=[{}]", phoneNumber);
            } else {
                log.warn("Customer not found for failed login sync, updated wallet only: {}", wallet.getWalletNumber());
            }

            return shouldBlock;
        } catch (Exception e) {
            log.error("Error handling failed login for wallet: {}", wallet.getWalletNumber(), e);
            return false;
        }
    }

    @Transactional
    public void resetFailedAttempts(Wallet wallet) {
        try {
            String phoneNumber = wallet.getPhoneNumber();

            // Reset failed attempts in wallet table
            if (wallet.getNumberTried() != null && wallet.getNumberTried() > 0) {
                wallet.setNumberTried(0);
                walletRepository.save(wallet);
                log.info("Reset failed attempts for wallet: {}", wallet.getWalletNumber());
            }

            // Also reset failed attempts in customer table if customer exists
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                if (customer.getNumberOfTries() != null && customer.getNumberOfTries() > 0) {
                    customer.setNumberOfTries(0);
                    customerRepository.save(customer);
                    log.info("Reset failed attempts in both wallet and customer tables for phoneNumber=[{}]", phoneNumber);
                }
            } else {
                log.warn("Customer not found for failed attempts reset sync, reset wallet only: {}", wallet.getWalletNumber());
            }
        } catch (Exception e) {
            log.error("Error resetting failed attempts for wallet: {}", wallet.getWalletNumber(), e);
        }
    }

    @Transactional
    public void recordSuccessfulLogin(Wallet wallet) {
        try {
            String phoneNumber = wallet.getPhoneNumber();

            // Update wallet table
            wallet.setNumberTried(0);
            wallet.setLastActionDate(new Date());
            walletRepository.save(wallet);

            // Also update customer table if customer exists
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phoneNumber);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setNumberOfTries(0);
                customerRepository.save(customer);
                log.info("Recorded successful login in both wallet and customer tables for phoneNumber=[{}]", phoneNumber);
            } else {
                log.warn("Customer not found for successful login sync, updated wallet only: {}", wallet.getWalletNumber());
            }

            log.info("Recorded successful login for wallet: {}", wallet.getWalletNumber());
        } catch (Exception e) {
            log.error("Error recording successful login for wallet: {}", wallet.getWalletNumber(), e);
        }
    }
}
