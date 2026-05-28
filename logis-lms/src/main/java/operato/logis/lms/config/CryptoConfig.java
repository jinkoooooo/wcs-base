package operato.logis.lms.config;

import operato.logis.lms.util.AesGcmUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 데이터 암호화용 Bean 등록
 * - 키 누락 시 AesGcmUtil 사용 불가
 */
@Configuration
public class CryptoConfig {
    /**
     * AES-GCM 유틸 빈 등록
     * - 센터주소 필드단위 암/복호화에 사용
     *
     * @param b64 외부 설정값 (암호화 키 Base64 문자열)
     * @return AesGcmUtil 인스턴스
     */
    @Bean(name = "addressAesGcmUtil")
    public AesGcmUtil addressEncryptor(@Value("${address.crypto.key.b64:}") String b64) {
        if (b64 == null || b64.isBlank()) {
            return new AesGcmUtil(new byte[32]) {
                @Override
                public byte[] encrypt(byte[] p, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.key.b64'. Set the environment variable APP_ADDRESS_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.key.b64' in application.properties");
                }

                @Override
                public byte[] decrypt(byte[] c, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.key.b64'. Set the environment variable APP_ADDRESS_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.key.b64' in application.properties");
                }
            };
        }
        byte[] key = java.util.Base64.getDecoder().decode(b64);
        return new AesGcmUtil(key);
    }

    /**
     * AES-GCM 유틸 빈 등록
     * - 엣지서버정보 필드단위 암/복호화에 사용
     *
     * @param b64 외부 설정값 (암호화 키 Base64 문자열)
     * @return AesGcmUtil 인스턴스
     */
    @Bean(name = "edgeServerAesGcmUtil")
    public AesGcmUtil edgeServerInfoEncryptor(@Value("${edge.server.crypto.key.b64:}") String b64) {
        if (b64 == null || b64.isBlank()) {
            return new AesGcmUtil(new byte[32]) {
                @Override
                public byte[] encrypt(byte[] p, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.edge.server.key.b64'. Set the environment variable APP_EDGE_SERVER_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.edge.server.key.b64' in application.properties");
                }

                @Override
                public byte[] decrypt(byte[] c, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.edge.server.key.b64'. Set the environment variable APP_EDGE_SERVER_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.edge.server.key.b64' in application.properties");
                }
            };
        }
        byte[] key = java.util.Base64.getDecoder().decode(b64);
        return new AesGcmUtil(key);
    }

    /**
     * AES-GCM 유틸 빈 등록
     * - 개인정보 필드단위 암/복호화에 사용
     *
     * @param b64 외부 설정값 (암호화 키 Base64 문자열)
     * @return AesGcmUtil 인스턴스
     */
    @Bean(name = "personalInfoAesGcmUtil")
    public AesGcmUtil personalInfoInfoEncryptor(@Value("${personal.info.crypto.key.b64:}") String b64) {
        if (b64 == null || b64.isBlank()) {
            return new AesGcmUtil(new byte[32]) {
                @Override
                public byte[] encrypt(byte[] p, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.personal.info.key.b64'. Set the environment variable APP_PERSONAL_INFO_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.personal.info.key.b64' in application.properties");
                }

                @Override
                public byte[] decrypt(byte[] c, byte[] iv, byte[] aad) {
                    throw new IllegalStateException("Missing required AES key 'crypto.personal.info.key.b64'. Set the environment variable APP_PERSONAL_INFO_AES_KEY_B64 (Base64-encoded 32-byte key) or define 'crypto.personal.info.key.b64' in application.properties");
                }
            };
        }
        byte[] key = java.util.Base64.getDecoder().decode(b64);
        return new AesGcmUtil(key);
    }

    /**
     * 암호화 키 식별자 빈 등록
     * - 신규 저장 시 컬럼에 기록하여 암호화 키 롤링에 사용
     */
    @Bean(name = "addressCryptoKeyId")
    public Short addressCryptoKeyId(@Value("${address.crypto.key.id:1}") short keyId) {
        return keyId;
    }

    @Bean(name = "edgeServerCryptoKeyId")
    public Short edgeServerCryptoKeyId(@Value("${edge.server.crypto.key.id:1}") short keyId) {
        return keyId;
    }

    @Bean(name = "personalInfoCryptoKeyId")
    public Short personalInfoCryptoKeyId(@Value("${personal.info.crypto.key.id:1}") short keyId) {
        return keyId;
    }
}