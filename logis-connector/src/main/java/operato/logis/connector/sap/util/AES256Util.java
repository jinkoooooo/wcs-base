package operato.logis.connector.sap.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AES256Util {

    // 32바이트(256bit) 키
    private final SecretKeySpec secretKeySpec;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding"; // CBC가 아닌 SAP 호환용 기본

    /**
     * 생성자에서 키 설정
     * @param secretKey 32바이트 AES 키 (UTF-8 인코딩 기준)
     */
    public AES256Util(String secretKey) {
        if (secretKey == null || secretKey.length() != 32) {
            throw new IllegalArgumentException("AES 키는 32자 (256비트) 여야 합니다.");
        }
        this.secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /**
     * 암호화
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES Encrypt Fails", e);
        }
    }

    /**
     * 복호화
     */
    public String decrypt(String encryptedBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES Decrypt fails", e);
        }
    }
}
