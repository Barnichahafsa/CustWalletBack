package org.bits.diamabankwalletf.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class PinEncryptionUtil {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 10;

    private final JdbcTemplate jdbcTemplate;

    // Cache for bank encryption keys
    private final Map<String, BankEncryptionKeys> bankKeysMap = new ConcurrentHashMap<>();


    /**
     * Get AES key from password - EXACT match to server implementation
     */
    private SecretKey getAESKeyFromPassword(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Convert hex string to bytes
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Get encryption keys for a specific bank
     */
    private BankEncryptionKeys getBankKeys(String bankCode) {
        log.info("Bank code: {}", bankCode);
        // Check cache first
        if (bankKeysMap.containsKey(bankCode)) {
            return bankKeysMap.get(bankCode);
        }

        try {
            // Get keys from database and decrypt them exactly like the server does
            String sql = "SELECT SECRET_KEY, VI FROM KEYS_MANAGEMENT WHERE BANK_CODE = ?";
            BankEncryptionKeys keys = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String encryptedSecretKey = rs.getString("SECRET_KEY");
                String encryptedIv = rs.getString("VI");

                try {
                    // Decrypt using 3DES, exactly like the server
                    String secretKey = decrypt3DES(encryptedSecretKey);
                    String iv = decrypt3DES(encryptedIv);

                    return new BankEncryptionKeys(secretKey, iv);
                } catch (Exception e) {
                    log.error("Error decrypting keys for bank: {}", bankCode, e);
                    throw new RuntimeException("Error decrypting keys", e);
                }
            }, bankCode);

            // Cache the keys
            if (keys != null) {
                bankKeysMap.put(bankCode, keys);
            }

            return keys;
        } catch (Exception e) {
            log.error("Failed to get encryption keys for bank: {}", bankCode, e);
            throw new RuntimeException("Could not retrieve encryption keys", e);
        }
    }

    /**
     * Decrypt using 3DES - exactly matches server implementation
     */
    private String decrypt3DES(String encryptedValue) throws Exception {
        // Load key from resources
        byte[] keyBytes = loadKeyFromResources();

        // Use TripleDES class exactly like the server
        TripleDES tripleDES = new TripleDES(keyBytes);
        return tripleDES.soften(encryptedValue);
    }

    private byte[] loadKeyFromResources() throws IOException {
        // Load key from WildFly standalone/configuration folder
        Path keyPath = Paths.get(System.getProperty("jboss.server.config.dir"), "key.3des.dat");

        if (!Files.exists(keyPath)) {
            log.error("Key file not found in standalone/configuration: key.3des.dat");
            throw new IOException("Key file not found in standalone/configuration: key.3des.dat");
        }

        log.info("Successfully loaded key file from standalone/configuration");
        return Files.readAllBytes(keyPath);
    }

 /*  private byte[] loadKeyFromResources() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/key.3des.dat")) {
            if (is == null) {
                log.error("Key file not found in resources: key.3des.dat");
                throw new IOException("Key file not found in resources: key.3des.dat");
            }

            log.info("Successfully loaded key file from resources");
            return is.readAllBytes();
        }
    }

*/
    /**
     * Inner class to hold bank encryption keys
     */
    private static class BankEncryptionKeys {
        private final String secretKey;
        private final String iv;

        public BankEncryptionKeys(String secretKey, String iv) {
            this.secretKey = secretKey;
            this.iv = iv;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public String getIv() {
            return iv;
        }
    }

    /**
     * TripleDES implementation - exact copy of server implementation
     */
    private static class TripleDES {

        byte[] key;

        public TripleDES(byte[] bs) {
            key = bs;
        }

        /**
         * Method To Encrypt The String - exact match to server
         */
        public String harden(String unencryptedString) throws Exception {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digestOfPassword = md.digest(key);
            byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

            for (int j = 0, k = 16; j < 8;) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey secretKey = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] plainTextBytes = unencryptedString.getBytes("utf-8");
            byte[] buf = cipher.doFinal(plainTextBytes);
            byte[] base64Bytes = Base64.encodeBase64(buf);
            String base64EncryptedString = new String(base64Bytes);

            return base64EncryptedString;
        }

        /**
         * Method To Decrypt An Encrypted String - exact match to server
         */
        public String soften(String encryptedString) throws Exception {
            if(encryptedString == null) {
                return "";
            }
            byte[] message = Base64.decodeBase64(encryptedString.getBytes("utf-8"));

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestOfPassword = md.digest(key);
            byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

            for (int j = 0, k = 16; j < 8;) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey secretKey = new SecretKeySpec(keyBytes, "DESede");

            Cipher decipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            decipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] plainText = decipher.doFinal(message);

            return new String(plainText, "UTF-8");
        }
    }

    public String hashPassword(String password, String userCode) {
        String hashword = null;
        try {
            // Use only the first 4 characters of userCode
            userCode = userCode.substring(0, Math.min(userCode.length(), 4));

            // Format password with prefix
            String formattedPassword = FormatPrefix(userCode, '?', 4, 8) + password;
            log.debug("Formatted password before hashing: {}", formattedPassword);

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(formattedPassword.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);

            // Critical: Don't pad to 32 characters, as this seems to be the issue
            // The server implementation is not padding the output
            // In this specific case we need to remove the leading "0" if present
            if (hashword.startsWith("0")) {
                hashword = hashword.substring(1);
            }

            log.debug("Final hashed result: {}", hashword);
        } catch (NoSuchAlgorithmException nsae) {
            log.error("Hashing algorithm not found", nsae);
        }
        return hashword;
    }


    public String encryptPin(String pin, String bankCode) {
        try {
            // Get the bank's encryption keys
            BankEncryptionKeys keys = getBankKeys(bankCode);
            String key = keys.getSecretKey();
            String vi = keys.getIv();

            log.debug("Encrypting PIN for bank: {}", bankCode);

            // Generate the AES key from password
            SecretKey secretKey = getAESKeyFromPassword(key.toCharArray(), vi.getBytes());

            // Use VI bytes for the IV
            byte[] iv = vi.getBytes(StandardCharsets.UTF_8);

            // Encrypt with GCM mode
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encryptedText = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));

            // Prefix IV to the encrypted data
            byte[] cipherTextWithIv = ByteBuffer.allocate(iv.length + encryptedText.length)
                    .put(iv)
                    .put(encryptedText)
                    .array();

            // Convert to hex string
            String result = bytesToHex(cipherTextWithIv);
            return result;
        } catch (Exception e) {
            log.error("Error encrypting PIN", e);
            throw new RuntimeException("Error encrypting PIN", e);
        }



    }
    public  String hashPin(String pin,String refFirst4) {
        String hashword = null;
        try {
            pin=FormatPrefix(refFirst4, '?', 4, 8)+pin;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(pin.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }
        return hashword;
    }

    public static String FormatPrefix(String valueToPad, char filler, int size,int iTotalPrefix) {
        String Result = "<";
        int index = 0;
        for (String str : valueToPad.split("")) {
            if (index < size) {
                Result = Result + str + ">";
                index++;
            }
        }
        if (Result.length() < iTotalPrefix) {
            for (int i = valueToPad.length(); i <= size; i++)
                Result = Result + filler;

        }

        return Result;
    }
}
