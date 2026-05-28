/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.system.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.annotation.Resource;
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 시큐리티 관련 설정
 *
 * @author shortstop
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
//@Order(SecurityProperties.BASIC_AUTH_ORDER)
class SecurityConfig {
	/**
	 * 환경설정 객체
	 */
	@Resource
	private Environment env;
	/**
	 * 디폴트 암호화 알고리즘 - SHA-256
	 */
//	private final String DEFAULT_PASSWORD_ENCODE_ALGORITHM = "SHA-256";

	/**
	 * 사용자 조회 서비스
	 */
	@Autowired
	private UserDetailsService userDetailsService;


    /**
     * 암호화 알고리즘 설정
     *
     * @return
     */
    @Bean
    PasswordEncoder passwordEncoder() {
		String algorithm = env.getProperty(SysConfigConstants.SECURITY_PASSWORD_ENCODER_ALGORITHM, "SHA-256");

		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("SHA-256", new Sha256PasswordEncoder("SHA-256"));
		encoders.put("bcrypt", PasswordEncoderFactories.createDelegatingPasswordEncoder());

		DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("SHA-256", encoders);
		delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(new Sha256PasswordEncoder(algorithm));

		return delegatingPasswordEncoder;

	}

//    @Bean
//    ProviderManager createProviderManager() {
//		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
//		daoAuthenticationProvider.setUserDetailsService(this.userDetailsService); // 사용자 세부정보 서비스 설정
//		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder()); // SHA-256 PasswordEncoder 설정
//
//		List<AuthenticationProvider> list = ValueUtil.toList(daoAuthenticationProvider);
//		ProviderManager providerManager = new ProviderManager(list);
//		return providerManager;
//	}
    

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 시큐리티 필터 체인 설정
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 공통 설정
        commonAuthConfigure(http);

        // 기본 인증 설정
        basicAuthConfigure(http);

        // OAuth2 설정 (필요한 경우)
        if (SysValueUtil.toBoolean(env.getProperty(SecConfigConstants.SECURTY_OAUTH2_ENABLED, SysConstants.FALSE_STRING))) {
            oauth2Configure(http);
        }

        return http.build();
    }

	/**
	 * 공통 시큐리티 설정
	 *
	 * @param http
	 * @throws Exception
	 */
	public void commonAuthConfigure(HttpSecurity http) throws Exception {
		http
		.csrf(csrf -> csrf
				.disable() // CSRF 보호 비활성화
		)
		.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/**").permitAll() // 모든 요청 허용
		);
	}


	/**
	 * 일반적으로 인증 후에 세션을 관리하는 방식의 시큐리티 설정
	 *
	 * @param http
	 * @throws Exception
	 */
	public void basicAuthConfigure(HttpSecurity http) throws Exception {
		http
		.authorizeHttpRequests(authorize -> authorize
				.anyRequest().authenticated() // 모든 요청에 대해 인증 필요
		)
		.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 필요한 경우 세션 생성 정책 설정
		)
		.securityContext(securityContext -> {
			securityContext.requireExplicitSave(false); // 보안 컨텍스트 설정
		})
		.headers(headers -> headers.
				frameOptions(frameOptions -> frameOptions.disable()));
	}

	/**
	 * 세션을 사용하지 않는 JWT 방식의 시큐리티 설정
	 *
	 * @param http
	 * @throws Exception
	 */
	/*private void jwtAuthConfigure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
		.formLogin().disable()
		.httpBasic().disable();
	}*/

	/**
	 * OAUTH2 인증 지원을 위한 시큐리티 설정
	 *
	 * @param http
	 * @throws Exception
	 */
	public void oauth2Configure(HttpSecurity http) throws Exception {
		// TODO
	}

}