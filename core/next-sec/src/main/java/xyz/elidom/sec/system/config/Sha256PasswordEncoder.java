package xyz.elidom.sec.system.config;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Sha256PasswordEncoder implements PasswordEncoder {

    private final String algorithm;

    public Sha256PasswordEncoder(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] digest = messageDigest.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm [" + algorithm + "]", e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.equals(encodedPassword);
    }
}