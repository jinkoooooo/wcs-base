///* Copyright © Nearsolution Inc. All rights reserved. */
//
//package xyz.elidom.sec.service;
//
//import java.math.BigInteger;
//import java.security.KeyFactory;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.security.spec.RSAPrivateKeySpec;
//import java.security.spec.RSAPublicKeySpec;
//
//import javax.crypto.Cipher;
//
//import org.apache.commons.codec.binary.Base64;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import xyz.elidom.exception.server.ElidomRuntimeException;
//import xyz.elidom.sys.util.FileUtil;
//
//@Component
//public class RSAEncoder {
//	private final int READ_SIZE = 500;
//	private final String RSA = "RSA";
//	private final String DELIMITER = "$";
//	private final String LICENSE_SPEC_PATH = "spec";
//	private final String LICENSE_PATH = "license";
//	private final String LICENSE_FILE_NAME = "LICENSE";
//
//	private final String PUBLIC_KEY_MODULUS = "PublicKeyModulus";
//	private final String PUBLIC_KEY_EXPONENT = "PublicKeyExponent";
//	private final String PRIVATE_KEY_MODULUS = "PrivateKeyModulus";
//	private final String PRIVATE_KEY_EXPONENT = "PrivateKeyExponent";
//
//	private String licenseSpecPath;
//	private String publicKeyModulus;
//	private String publicKeyExponent;
//	private String privateKeyModulus;
//	private String privateKeyExponent;
//	private String decryptedText;
//
//	/**
//	 * Public, Private에 대한 License Spec 생성
//	 */
//	public void generateLicenseSpec() {
//		KeyPair keyPair = this.generateKeyPair();
//		PublicKey publicKey = keyPair.getPublic(); // 공개키
//		PrivateKey privateKey = keyPair.getPrivate(); // 개인키
//
//		try {
//			/**
//			 * Public Spec
//			 */
//			RSAPublicKeySpec publicKeySpec = this.getRSAPublicKeySpec(publicKey);
//			String publicKeyModulus = publicKeySpec.getModulus().toString(16);
//			String publicKeyExponent = publicKeySpec.getPublicExponent().toString(16);
//
//			/**
//			 * Private Spec
//			 */
//			RSAPrivateKeySpec privateKeySpec = this.getRSAPrivateKeySpec(privateKey);
//			String privateKeyModulus = privateKeySpec.getModulus().toString(16);
//			String privateKeyExponent = privateKeySpec.getPrivateExponent().toString(16);
//
//			/**
//			 * File 생성
//			 */
//			String path = this.getResourcePath(LICENSE_SPEC_PATH);
//
//			FileUtil.createFile(path, PUBLIC_KEY_MODULUS, publicKeyModulus);
//			FileUtil.createFile(path, PUBLIC_KEY_EXPONENT, publicKeyExponent);
//			FileUtil.createFile(path, PRIVATE_KEY_MODULUS, privateKeyModulus);
//			FileUtil.createFile(path, PRIVATE_KEY_EXPONENT, privateKeyExponent);
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	/**
//	 * Public Key로 암호화한 후 결과로 출력된 byte 배열을 Base64로 인코딩하여 String으로 변환하여 리턴함
//	 * 
//	 * @param text
//	 * @return Base64로 인코딩된 암호화 문자열
//	 */
//	public String encrypt(String text) {
//		String publicKeyModulus = this.getPublicKeyModulus();
//		String publicKeyExponent = this.getPublicKeyExponent();
//
//		BigInteger modulus = new BigInteger(publicKeyModulus, 16);
//		BigInteger exponent = new BigInteger(publicKeyExponent, 16);
//
//		RSAPublicKeySpec pubks = new RSAPublicKeySpec(modulus, exponent);
//
//		try {
//			return this.encrypt(text, KeyFactory.getInstance(RSA).generatePublic(pubks));
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	private String encrypt(String text, PublicKey publicKey) {
//		try {
//			Cipher cipher = Cipher.getInstance(RSA);
//			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//
//			int length = text.length();
//			int start = 0;
//			int end = 0;
//
//			StringBuilder encryptText = new StringBuilder();
//
//			do {
//				end = start + READ_SIZE;
//				if (end > length) {
//					end = length;
//				}
//
//				String value = text.substring(start, end);
//				encryptText.append(new String(Base64.encodeBase64(cipher.doFinal(value.getBytes())))).append(DELIMITER);
//
//				start = end;
//			} while (end < length);
//
//			// File 생성
//			StringBuilder path = new StringBuilder();
//			path.append(this.getResourcePath(""));
//			path.append(LICENSE_PATH);
//
//			FileUtil.createFile(path.toString(), LICENSE_FILE_NAME, encryptText.toString());
//
//			return encryptText.toString();
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	/**
//	 * decode 시킨 후 RSA 비밀키(Private Key)를 이용하여 암호화된 텍스트를 원문으로 복호화
//	 * 
//	 * @return
//	 */
//	public String decrypt() {
//		return this.decrypt(false);
//	}
//
//	public String decrypt(boolean isRefresh) {
//		if (decryptedText != null && !isRefresh)
//			return decryptedText;
//
//		String privateKeyModulus = this.getPrivateKeyModulus();
//		String privateKeyExponent = this.getPrivateKeyExponent();
//
//		BigInteger modulus = new BigInteger(privateKeyModulus, 16);
//		BigInteger exponent = new BigInteger(privateKeyExponent, 16);
//
//		RSAPrivateKeySpec priks = new RSAPrivateKeySpec(modulus, exponent);
//
//		try {
//			String dirPath = this.getResourcePath(LICENSE_PATH);
//			String encryptedText = FileUtil.readFileContent(dirPath, LICENSE_FILE_NAME);
//			return this.decrypt(encryptedText, KeyFactory.getInstance(RSA).generatePrivate(priks));
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	private String decrypt(String encryptedText, PrivateKey privateKey) {
//		try {
//			Cipher cipher = Cipher.getInstance(RSA);
//			cipher.init(Cipher.DECRYPT_MODE, privateKey);
//
//			String[] strs = StringUtils.tokenizeToStringArray(encryptedText, DELIMITER);
//
//			StringBuilder sb = new StringBuilder();
//			for (String str : strs) {
//				byte[] bytes = Base64.decodeBase64(str.getBytes());
//				sb.append(new String(cipher.doFinal(bytes)));
//			}
//
//			decryptedText = sb.toString();
//			return decryptedText;
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	private KeyPair generateKeyPair() {
//		try {
//			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
//			keyPairGenerator.initialize(4096);
//			return keyPairGenerator.genKeyPair();
//		} catch (Exception e) {
//			throw new ElidomRuntimeException(e);
//		}
//	}
//
//	/**
//	 * 
//	 * RSA 공개키로부터 RSAPublicKeySpec 객체를 생성함
//	 * 
//	 * @param publicKey
//	 *            공개키
//	 * 
//	 * @return RSAPublicKeySpec spec
//	 * 
//	 */
//
//	private RSAPublicKeySpec getRSAPublicKeySpec(PublicKey publicKey) throws Exception {
//		return KeyFactory.getInstance(RSA).getKeySpec(publicKey, RSAPublicKeySpec.class);
//	}
//
//	/**
//	 * 
//	 * RSA 비밀키로부터 RSAPrivateKeySpec 객체를 생성함
//	 * 
//	 * @param privateKey
//	 *            비밀키
//	 * 
//	 * @return RSAPrivateKeySpec
//	 * 
//	 */
//	private RSAPrivateKeySpec getRSAPrivateKeySpec(PrivateKey privateKey) throws Exception {
//		return KeyFactory.getInstance(RSA).getKeySpec(privateKey, RSAPrivateKeySpec.class);
//	}
//
//	private String getResourcePath(String resource) {
//		StringBuilder path = new StringBuilder();
//		path.append(getClass().getClassLoader().getResource(resource).getFile());
//		return path.toString().replace("bin/", "src/main/resources/");
//	}
//
//	private String getLicenseSpecPath() {
//		if (licenseSpecPath == null) {
//			licenseSpecPath = this.getResourcePath(LICENSE_SPEC_PATH);
//		}
//		return licenseSpecPath;
//	}
//
//	private String getPublicKeyModulus() {
//		if (publicKeyModulus == null) {
//			publicKeyModulus = FileUtil.readFileContent(this.getLicenseSpecPath(), PUBLIC_KEY_MODULUS);
//		}
//		return publicKeyModulus;
//	}
//
//	private String getPublicKeyExponent() {
//		if (publicKeyExponent == null) {
//			publicKeyExponent = FileUtil.readFileContent(this.getLicenseSpecPath(), PUBLIC_KEY_EXPONENT);
//		}
//		return publicKeyExponent;
//	}
//
//	private String getPrivateKeyModulus() {
//		if (privateKeyModulus == null) {
//			privateKeyModulus = FileUtil.readFileContent(this.getLicenseSpecPath(), PRIVATE_KEY_MODULUS);
//		}
//		return privateKeyModulus;
//	}
//
//	private String getPrivateKeyExponent() {
//		if (privateKeyExponent == null) {
//			privateKeyExponent = FileUtil.readFileContent(this.getLicenseSpecPath(), PRIVATE_KEY_EXPONENT);
//		}
//		return privateKeyExponent;
//	}
//}