package xyz.elidom.sys.system.config;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

    @Bean
    StringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("nearsolution"); // 암호화 키 설정
        encryptor.setAlgorithm("PBEWithMD5AndDES"); // 알고리즘 설정
        return encryptor;
    }
}