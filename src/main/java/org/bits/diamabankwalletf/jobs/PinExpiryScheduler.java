package org.bits.diamabankwalletf.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.service.PinExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PinExpiryScheduler {

    private final PinExpiryService pinExpiryService;

    /**
     * Job quotidien pour gérer l'expiration des PINs
     * Exécuté tous les jours à 09:00
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void processPinExpiryNotifications() {
        log.info("Starting daily PIN expiry notification job");

        try {
            // 1. Marquer les PINs expirés pour changement forcé
            pinExpiryService.markExpiredPinsForChange();

            // 2. Envoyer notifications 7 jours avant expiration
            pinExpiryService.sendSevenDayExpiryNotifications();

            // 3. Envoyer notifications 3 jours avant expiration
            pinExpiryService.sendThreeDayExpiryNotifications();

            // 4. Envoyer notifications 1 jour avant expiration
            pinExpiryService.sendOneDayExpiryNotifications();

            // 5. Afficher les statistiques
            PinExpiryService.PinExpiryStats stats = pinExpiryService.getPinExpiryStats();
            log.info("PIN expiry job completed - Expired: {}, Expiring today: {}",
                    stats.getExpiredCount(), stats.getExpiringTodayCount());

        } catch (Exception e) {
            log.error("Error during PIN expiry notification job", e);
        }
    }

    /**
     * Job de contrôle des PINs expirés (toutes les 4 heures)
     * Pour s'assurer qu'aucun PIN expiré n'est oublié
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void checkExpiredPins() {
        log.info("Checking for expired PINs");

        try {
            pinExpiryService.markExpiredPinsForChange();

            PinExpiryService.PinExpiryStats stats = pinExpiryService.getPinExpiryStats();
            if (stats.getExpiredCount() > 0) {
                log.warn("Found {} wallets with expired PINs requiring change", stats.getExpiredCount());
            }

        } catch (Exception e) {
            log.error("Error during expired PIN check", e);
        }
    }

    /**
     * Job de rapport hebdomadaire (dimanche à 18:00)
     */
    @Scheduled(cron = "0 0 18 * * SUN")
    public void weeklyPinExpiryReport() {
        log.info("Generating weekly PIN expiry report");

        try {
            PinExpiryService.PinExpiryStats stats = pinExpiryService.getPinExpiryStats();

            log.info("=== WEEKLY PIN EXPIRY REPORT ===");
            log.info("Wallets with expired PINs: {}", stats.getExpiredCount());
            log.info("Wallets with PINs expiring today: {}", stats.getExpiringTodayCount());
            log.info("================================");

        } catch (Exception e) {
            log.error("Error generating weekly PIN expiry report", e);
        }
    }
}
