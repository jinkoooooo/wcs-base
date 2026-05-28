/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.sys.system.auth.model;

/**
 * 패스워드를 체크하기 위한 모델 
 * 
 * @author Minu.Kim
 */
public class CheckPassword {
	
	private String login;
	private String name;
	private String currentPass;
	private String newPass;
	private String email;

	public String getLogin() {
		return login;
	}
	
	public void setLogin(String login) {
		this.login = login;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCurrentPass() {
		return currentPass;
	}

	public void setCurrentPass(String currentPass) {
		this.currentPass = currentPass;
	}

	public String getNewPass() {
		return newPass;
	}

	public void setNewPass(String newPass) {
		this.newPass = newPass;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}