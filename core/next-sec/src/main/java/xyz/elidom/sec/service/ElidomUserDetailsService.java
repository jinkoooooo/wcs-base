/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sec.model.ElidomUserDetails;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 인증시에 스프링에 제공할 사용자 정보 조회를 위한 서비스
 * 
 * @author shortstop
 */
@Service
public class ElidomUserDetailsService implements UserDetailsService {
	
	/**
	 * 쿼리 매니저
	 */
	@Autowired
	private IQueryManager queryManager;

	/**
	 * 기본 메시지 EMPTY_NOT_ALLOWED - Empty [{0}] is not allowed
	 */
	private static final String MSG_EMPTY_NOT_ALLOWED = "Empty [{0}] is not allowed!";
	/**
	 * 사용자 아이디 다국어 코드 
	 */
	private static final String USER_ID = "terms.label.user_id";
	
	/**
	 * AuthenticationManager에서 인증 처리하는 과정에서 호출됨
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
		if (SysValueUtil.isEmpty(username)) {
			String msg = MessageUtil.getMessage(SysMessageConstants.EMPTY_PARAM, MSG_EMPTY_NOT_ALLOWED, MessageUtil.params(USER_ID));
			throw new UsernameNotFoundException(msg);
		}

		User user = this.queryManager.select(User.class, username);
		
		if (SysValueUtil.isEmpty(user)) {
			throw new UsernameNotFoundException("User [" + username + "] is not exist.");
		}
		
		// 인증 성공 후 사용자를 찾았다면 Authentication 객체에 포함되는 UserDetail 객체를 제공
		return new ElidomUserDetails(user);
	}
}