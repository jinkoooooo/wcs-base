package operato.logis.lms.util;

import operato.logis.lms.consts.EncryptionDomain;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class AesGcmEncryptor {
    private final Map<String, AesGcmUtil> encryptorMap;
    private final Map<String, Short> keyIdMap;

    public AesGcmEncryptor(Map<String, AesGcmUtil> encryptorMap, Map<String, Short> keyIdMap) {
        this.encryptorMap = encryptorMap;
        this.keyIdMap = keyIdMap;
    }

    /**
     * 암호화
     *
     * @param plaintext 암호화 할 평문
     * @param aad
     * @param domain    사용할 암호화 빈 종류
     * @return
     */
    public EncryptedData encrypt(String plaintext, byte[] aad, EncryptionDomain domain) {
        AesGcmUtil util = getEncryptor(domain);
        Short keyId = getKeyId(domain);

        if (ObjectUtils.isEmpty(plaintext)) {
            throw new IllegalArgumentException("plaintext must not be empty");
        }

        try {
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] iv = util.randomIv();
            byte[] cipherText = util.encrypt(plaintextBytes, iv, aad);

            return new EncryptedData(cipherText, iv, keyId);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed for type: " + domain, e);
        }
    }

    /**
     * 복호화
     *
     * @param cipherText 암호문
     * @param iv
     * @param domain     사용할 복호화 빈 종류
     * @return
     */
    public String decrypt(byte[] cipherText, byte[] iv, byte[] aad, EncryptionDomain domain) {
        AesGcmUtil util = getEncryptor(domain);

        if (util == null) {
            throw new IllegalArgumentException("Unknown encryption type: " + domain);
        }

        try {
            byte[] plaintextBytes = util.decrypt(cipherText, iv, aad);

            // UTF-8 문자열 반환. 바이트 -> 문자열
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed for type: " + domain, e);
        }
    }

    /**
     * 유틸
     */
    private AesGcmUtil getEncryptor(EncryptionDomain domain) {
        return Optional.ofNullable(encryptorMap.get(domain.getValue() + "AesGcmUtil"))
                .orElseThrow(() -> new IllegalArgumentException("No encryptor for: " + domain));
    }

    private Short getKeyId(EncryptionDomain domain) {
        return Optional.ofNullable(keyIdMap.get(domain.getValue() + "CryptoKeyId"))
                .orElseThrow(() -> new IllegalArgumentException("No keyId for: " + domain));
    }
}
