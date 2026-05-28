package operato.logis.lms.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM(Authenticated Encryption) 유틸
 */
public class AesGcmUtil {

    public static final int GCM_IV_LENGTH_BYTE = 12;                    // 12-byte(=96-bit), GCM IV 길이
    public static final int GCM_TAG_LENGTH_BIT = 128;                   // 16-byte(=128-bit), GCM 인증 태그 길이

    private static final int KEY_SIZE = 256;                            // 32 byte(=256-bit), AES-256 키 길이
    private static final String ALGORITHM = "AES";                      // 대칭키 알고리즘 명
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";   // JCA 변환 명

    private final SecretKey secretKey;                                  // 프로세스 내 재사용되는 대칭키
    private final SecureRandom secureRandom = new SecureRandom();       // IV 생성용 난수생성기(CSPRNG)

    /**
     * @param rawKey 32-byte AES-256 키. 환경변수 Base64 디코드 결과값
     */
    public AesGcmUtil(byte[] rawKey) {
        if (rawKey == null || rawKey.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes");
        }
        this.secretKey = new SecretKeySpec(rawKey, ALGORITHM);
    }

    /**
     * 난수 키 생성 보조유틸
     *
     * @return 32-byte 키
     * @throws Exception
     */
    public static byte[] randomKey256() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey().getEncoded();
    }

    /**
     * Base64 디코드 보조 유틸
     *
     * @param b64 Base64 문자열
     * @return 디코드된 바이트 배열
     */
    public static byte[] b64Decode(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    /**
     * 초기화 벡터 (IV, Initialization Vector) 생성
     * - 동일 키로 암호화 시 재사용 금지 (Nonce)
     * - 동일 키로 암호화 시 컬럼 간 교차 사용 금지
     *
     * @return IV
     */
    public byte[] randomIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTE];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * 암호화
     *
     * @param plaintext 평문 바이트 (Null 금지)
     * @param iv        초기화 벡터
     * @param aad       변조 방지용 추가 인증 데이터. 미사용 시 null
     * @return 암호문||인증태그 결합된 형식의 바이트 배열
     * @throws Exception
     */
    public byte[] encrypt(byte[] plaintext, byte[] iv, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        if (aad != null) cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    /**
     * 복호화
     *
     * @param ciphertextWithTag 암호문||인증태그 결합된 형식의 바이트 배열
     * @param iv                암호화 시 사용한 초기화 벡터
     * @param aad               암호화 시 사용한 변조 방지용 추가 인증 데이터. 미사용 시 null
     * @return 평문 바이트
     * @throws Exception AAD/IV/KEY 불일치 또는 데이터 변조 시 발생
     */
    public byte[] decrypt(byte[] ciphertextWithTag, byte[] iv, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        if (aad != null) cipher.updateAAD(aad);
        return cipher.doFinal(ciphertextWithTag);
    }
}