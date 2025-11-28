package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.utils.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityQuestionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get security question for a wallet from database
     */
    public Map<String, Object> getSecurityQuestion(Wallet wallet) {
        try {
            String questionCode = wallet.getSecretQuestion();

            if (questionCode == null || questionCode.isEmpty()) {
                log.warn("No security question set for wallet: {}", wallet.getWalletNumber());
                return null;
            }

            // Query the SECRET_QUESTION table
            String sql = "SELECT QUESTION FROM SECRET_QUESTION WHERE ID = ?";
            String questionText = jdbcTemplate.queryForObject(sql, String.class, questionCode);

            if (questionText == null) {
                log.warn("Question not found in database for wallet: {}, questionCode: {}",
                        wallet.getWalletNumber(), questionCode);
                return null;
            }

            return Map.of(
                    "id", questionCode,
                    "question", questionText,
                    "type", "text"
            );

        } catch (Exception e) {
            log.error("Error fetching security question from database for wallet: {}",
                    wallet.getWalletNumber(), e);
            return null;
        }
    }

    /**
     * Verify security question answer
     */
    public boolean verifySecurityAnswer(Wallet wallet, String providedAnswer) {
        try {
            String storedAnswer = wallet.getAnswer();

            if (storedAnswer == null || storedAnswer.isEmpty()) {
                log.warn("No stored security answer for wallet: {}", wallet.getWalletNumber());
                return false;
            }

            if (providedAnswer == null || providedAnswer.trim().isEmpty()) {
                log.warn("No answer provided for wallet: {}", wallet.getWalletNumber());
                return false;
            }

            // Use client code first 4 chars as salt (consistent with PIN verification)
            String salt = wallet.getClientCode().substring(0, Math.min(4, wallet.getClientCode().length()));

            // Try to verify assuming it's hashed
            if (storedAnswer.length() > 20) { // Likely hashed
                return SecurityUtils.verifySecurityAnswer(providedAnswer, storedAnswer, salt);
            } else {
                // Direct comparison for legacy plain text answers
                String normalizedProvided = providedAnswer.trim().toLowerCase();
                String normalizedStored = storedAnswer.trim().toLowerCase();
                return normalizedProvided.equals(normalizedStored);
            }

        } catch (Exception e) {
            log.error("Error verifying security answer for wallet: {}", wallet.getWalletNumber(), e);
            return false;
        }
    }

    /**
     * Check if wallet has a security question set
     */
    public boolean hasSecurityQuestion(Wallet wallet) {
        return wallet.getSecretQuestion() != null &&
                !wallet.getSecretQuestion().isEmpty() &&
                wallet.getAnswer() != null &&
                !wallet.getAnswer().isEmpty();
    }

    /**
     * Get all available security questions for selection
     */
    public java.util.List<Map<String, Object>> getAllSecurityQuestions() {
        try {
            String sql = "SELECT ID, QUESTION FROM SECRET_QUESTION WHERE STATUS = 'Y' ORDER BY ID";

            return jdbcTemplate.query(sql, (rs, rowNum) -> Map.of(
                    "id", rs.getString("ID"),
                    "question", rs.getString("QUESTION")
            ));

        } catch (Exception e) {
            log.error("Error fetching all security questions from database", e);
            return java.util.List.of();
        }
    }
}
