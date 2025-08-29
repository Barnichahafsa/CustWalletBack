package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinExpiryService {

    private final WalletRepository walletRepository;
    private final NotificationService notificationService;

    @Value("${wallet.pin.expiry.months:3}")
    private int pinExpiryMonths;

    /**
     * Initialise les dates d'expiration pour les nouveaux PINs
     */
    @Transactional
    public void initializePinExpiry(Wallet wallet) {
        Date now = new Date();

        // Définir la date de dernière modification du PIN
        wallet.setPinLastChangedDate(now);

        // Calculer la date d'expiration (3 mois par défaut)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MONTH, pinExpiryMonths);
        wallet.setPinExpiryDate(calendar.getTime());

        // Réinitialiser les flags
        wallet.setPinChangeRequired('N');
        wallet.setPinExpiryNotificationSent('N');

        walletRepository.save(wallet);

        log.info("PIN expiry initialized for wallet: {}, expiry date: {}",
                wallet.getWalletNumber(), wallet.getPinExpiryDate());
    }

    /**
     * Vérifie si le PIN a expiré
     */
    public boolean isPinExpired(Wallet wallet) {
        log.info("=== DEBUG isPinExpired ===");
        log.info("Wallet number: {}", wallet.getWalletNumber());
        log.info("PIN expiry date from wallet: {}", wallet.getPinExpiryDate());
        log.info("PIN expiry date is null: {}", wallet.getPinExpiryDate() == null);

        if (wallet.getPinExpiryDate() == null) {
            log.info("PIN expiry date is null, returning false");
            return false; // Si pas de date d'expiration, considérer comme non expiré
        }

        Date now = new Date();
        boolean expired = now.after(wallet.getPinExpiryDate());

        log.info("Current date: {}", now);
        log.info("PIN expiry date: {}", wallet.getPinExpiryDate());
        log.info("Is expired: {}", expired);

        if (expired) {
            log.warn("PIN expired for wallet: {}, expiry date was: {}",
                    wallet.getWalletNumber(), wallet.getPinExpiryDate());
        }

        return expired;
    }
    /**
     * Vérifie si le changement de PIN est requis
     */
    public boolean isPinChangeRequired(Wallet wallet) {
        return wallet.getPinChangeRequired() != null && wallet.getPinChangeRequired() == 'Y';
    }

    /**
     * Force le changement de PIN pour un portefeuille
     */
    @Transactional
    public void forcePinChange(Wallet wallet) {
        wallet.setPinChangeRequired('Y');
        walletRepository.save(wallet);

        log.info("PIN change forced for wallet: {}", wallet.getWalletNumber());
    }

    /**
     * Met à jour les portefeuilles avec PIN expiré
     */
    @Transactional
    public void markExpiredPinsForChange() {
        List<Wallet> expiredWallets = walletRepository.findWalletsWithExpiredPin();

        log.info("Found {} wallets with expired PINs", expiredWallets.size());

        for (Wallet wallet : expiredWallets) {
            forcePinChange(wallet);
        }
    }

    /**
     * Envoie les notifications d'expiration (7 jours)
     */
    @Transactional
    public void sendSevenDayExpiryNotifications() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        Date sevenDaysFromNow = calendar.getTime();

        List<Wallet> wallets = walletRepository.findWalletsNeedingSevenDayNotification(sevenDaysFromNow);

        log.info("Sending 7-day expiry notifications to {} wallets", wallets.size());

        for (Wallet wallet : wallets) {
            sendPinExpiryNotification(wallet, 7);
            wallet.setPinExpiryNotificationSent('7');
            walletRepository.save(wallet);
        }
    }

    /**
     * Envoie les notifications d'expiration (3 jours)
     */
    @Transactional
    public void sendThreeDayExpiryNotifications() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        Date threeDaysFromNow = calendar.getTime();

        List<Wallet> wallets = walletRepository.findWalletsNeedingThreeDayNotification(threeDaysFromNow);

        log.info("Sending 3-day expiry notifications to {} wallets", wallets.size());

        for (Wallet wallet : wallets) {
            sendPinExpiryNotification(wallet, 3);
            wallet.setPinExpiryNotificationSent('3');
            walletRepository.save(wallet);
        }
    }

    /**
     * Envoie les notifications d'expiration (1 jour)
     */
    @Transactional
    public void sendOneDayExpiryNotifications() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date oneDayFromNow = calendar.getTime();

        List<Wallet> wallets = walletRepository.findWalletsNeedingOneDayNotification(oneDayFromNow);

        log.info("Sending 1-day expiry notifications to {} wallets", wallets.size());

        for (Wallet wallet : wallets) {
            sendPinExpiryNotification(wallet, 1);
            wallet.setPinExpiryNotificationSent('1');
            walletRepository.save(wallet);
        }
    }

    /**
     * Envoie une notification d'expiration de PIN
     */
    private void sendPinExpiryNotification(Wallet wallet, int daysRemaining) {
        try {
            String message = buildExpiryMessage(daysRemaining);
            String phoneNumber = wallet.getPhoneNumber() != null ?
                    wallet.getPhoneNumber() : wallet.getMobileNumber();

            // TODO: Intégrer avec votre service de notification SMS/Push
            // notificationService.sendSMS(phoneNumber, message);
            // notificationService.sendPushNotification(wallet.getClientCode(), message);

            log.info("PIN expiry notification sent to wallet: {}, days remaining: {}",
                    wallet.getWalletNumber(), daysRemaining);

        } catch (Exception e) {
            log.error("Failed to send PIN expiry notification for wallet: {}",
                    wallet.getWalletNumber(), e);
        }
    }

    /**
     * Construit le message de notification
     */
    private String buildExpiryMessage(int daysRemaining) {
        if (daysRemaining == 1) {
            return "ALERTE : Votre code PIN expire demain ! Connectez-vous à votre app DIAMA Wallet pour le modifier.";
        } else {
            return String.format("ALERTE : Votre code PIN expire dans %d jours ! Connectez-vous à votre app DIAMA Wallet pour le modifier.",
                    daysRemaining);
        }
    }

    /**
     * Méthode pour réinitialiser après changement de PIN
     */
    @Transactional
    public void pinChanged(Wallet wallet) {
        initializePinExpiry(wallet);
        log.info("PIN expiry reset after PIN change for wallet: {}", wallet.getWalletNumber());
    }

    /**
     * Obtient des statistiques sur l'expiration des PINs
     */
    public PinExpiryStats getPinExpiryStats() {
        Long expiringToday = walletRepository.countWalletsWithPinExpiringToday();
        List<Wallet> expiredWallets = walletRepository.findWalletsWithExpiredPin();

        return new PinExpiryStats(
                expiredWallets.size(),
                expiringToday.intValue()
        );
    }

    /**
     * Classe pour les statistiques d'expiration
     */
    public static class PinExpiryStats {
        private final int expiredCount;
        private final int expiringTodayCount;

        public PinExpiryStats(int expiredCount, int expiringTodayCount) {
            this.expiredCount = expiredCount;
            this.expiringTodayCount = expiringTodayCount;
        }

        public int getExpiredCount() { return expiredCount; }
        public int getExpiringTodayCount() { return expiringTodayCount; }
    }
}
