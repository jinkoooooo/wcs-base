package operato.logis.lms.util;

public class EncryptedData {
    private final byte[] cipherText;    // '암호문||인증태그' 형식
    private final byte[] iv;            // IV
    private final Short keyId;          // 키 버전

    public EncryptedData(byte[] cipherText, byte[] iv, Short keyId) {
        this.cipherText = cipherText;
        this.iv = iv;
        this.keyId = keyId;
    }

    public byte[] getCipherText() { return cipherText; }
    public byte[] getIv() { return iv; }
    public Short getKeyId() { return keyId; }
}
