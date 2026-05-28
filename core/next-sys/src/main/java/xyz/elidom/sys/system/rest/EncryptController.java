package xyz.elidom.sys.system.rest;

import java.util.HashMap;
import java.util.Map;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
public class EncryptController {

	@Autowired
	StringEncryptor stringEncryptor;

	private final String PREFIX = "ENC(";
	private final String SUBFIX = ")";
	private final String KEY = "value";

	/**
	 * Encrypt Value.
	 * 
	 * @param value
	 * @return
	 */
	@GetMapping("/encrypt/{value:.+}")
	public String encrypt(@PathVariable String value) {
		return new StringBuilder(value).append(" : ").append(this.encryptValue(value)).toString();
	}

	/**
	 * Encrypt Value.
	 * 
	 * @param params
	 * @return
	 */
	@PostMapping("/encrypt")
	public Map<String, String> encrypt(@RequestBody Map<String, String> params) {
		String value = this.getValue(params);
		if (value == null)
			return null;

		Map<String, String> result = new HashMap<String, String>();
		result.put(value, this.encryptValue(value));
		return result;
	}

	/**
	 * Decrypt Value.
	 * 
	 * @param value
	 * @return
	 */
	@PostMapping(value = "/rest/decrypt/{value:.+}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String decrypt(@PathVariable String value) {
		String encryptValue = value;
		if (value.startsWith(PREFIX))
			encryptValue = value.substring(value.indexOf("(") + 1, value.lastIndexOf(SUBFIX));

		return new StringBuilder(value).append(" : ").append(stringEncryptor.decrypt(encryptValue)).toString();
	}

	/**
	 * params 에서 Value Key에 해당하는 값 가져오기 실행.
	 * 
	 * @param params
	 * @return
	 */
	private String getValue(Map<String, String> params) {
		return params == null ? null : params.get(KEY);
	}

	/**
	 * Value 값을 Encrypt 실행.
	 * 
	 * @param value
	 * @return
	 */
	private String encryptValue(String value) {
		return new StringBuilder().append(PREFIX).append(stringEncryptor.encrypt(value)).append(SUBFIX).toString();
	}
}