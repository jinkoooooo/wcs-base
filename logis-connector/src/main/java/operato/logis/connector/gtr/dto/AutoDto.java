package operato.logis.connector.gtr.dto;

import lombok.Data;

public class AutoDto {

    // 로그인 요청
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    // 로그인 응답 (토큰 갱신 응답에도 사용)
    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
    }

    // 토큰 갱신/로그아웃 요청
    @Data
    public static class RefreshOrLogoutRequest {
        private String refreshToken;
    }
}
