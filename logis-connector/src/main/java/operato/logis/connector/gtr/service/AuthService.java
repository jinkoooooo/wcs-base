package operato.logis.connector.gtr.service;

import operato.logis.connector.gtr.dto.AutoDto;
import operato.logis.connector.gtr.entity.GtrToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
public class AuthService extends AbstractQueryService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final WebClient webClient;
    private final TokenService tokenService;
    public static final Long DOMAIN_ID = 7L;
    public static final String DOMAIN_NAME = "mw";

    public AuthService(@Qualifier("externalApiWebClient") WebClient webClient, TokenService tokenService) {
        this.webClient = webClient;
        this.tokenService = tokenService;
    }

    /**
     * 59분마다 토큰 자동 갱신
     */
    public void autoTokenRefresh() {
        log.info("[Auto-Refresh] 스케줄러 실행 시작...");

        // 1. DB에서 현재 토큰 조회
        GtrToken token = tokenService.getToken();

        if (token == null || token.getRefreshToken() == null) {
            log.warn("[Auto-Refresh] 저장된 토큰이 없어 갱신을 건너뜁니다.");
            return;
        }

        // 2. 요청 객체 생성
        AutoDto.RefreshOrLogoutRequest request = new AutoDto.RefreshOrLogoutRequest();
        request.setRefreshToken(token.getRefreshToken());

        // 3. 갱신 로직 실행 및 구독(Subscribe)
        this.refreshToken(request, token)
                .subscribe(
                        response -> log.info("Auto-Refresh] 완료. (New AccessToken: {})",
                                response.getAccessToken().substring(0, 5) + "..."),
                        error -> log.error("[Auto-Refresh] 실패: ", error)
                );
    }

    /**
     * POST /api/v1/auth/login
     */
    public Mono<AutoDto.AuthResponse> login(AutoDto.LoginRequest request) {
        return webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("🚨 [Login Fail] Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("GTR 로그인 실패: " + clientResponse.statusCode()));
                                })
                )
                .bodyToMono(AutoDto.AuthResponse.class)
                .flatMap(authResponse -> {
                    // DB 저장/업데이트 (블로킹 작업)를 별도 스레드로 격리
                    return Mono.fromRunnable(() -> {
                                try {
                                    log.info("✅ [Login Success] Token issued. DB에 저장을 시작합니다.");

                                    // 1. 시스템 도메인 세팅 (기존 로직과 동일)
                                    Domain domain = new Domain();
                                    domain.setId(DOMAIN_ID);
                                    domain.setName(DOMAIN_NAME);
                                    Domain.setCurrentDomain(domain);

                                    // 2. DB에 기존 토큰이 있는지 확인
                                    GtrToken existingToken = tokenService.getToken();

                                    if (existingToken != null) {
                                        // 3-A. 기존 토큰이 존재하면 Update
                                        existingToken.setAccessToken(authResponse.getAccessToken());
                                        existingToken.setRefreshToken(authResponse.getRefreshToken());

                                        this.queryManager.update(existingToken, "accessToken", "refreshToken");
                                        log.info("✅ [Login] 기존 토큰 DB Update 완료 (ID: {})", existingToken.getId());
                                    } else {
                                        // 3-B. 기존 토큰이 없으면 새로 Insert
                                        GtrToken newToken = new GtrToken();
                                        newToken.setAccessToken(authResponse.getAccessToken());
                                        newToken.setRefreshToken(authResponse.getRefreshToken());

                                        // ID 생성 전략에 따라 필요시 newToken.setId(...) 추가
                                        this.queryManager.insert(newToken);
                                        log.info("✅ [Login] 신규 토큰 DB Insert 완료");
                                    }

                                } catch (Exception e) {
                                    log.error("❌ 로그인 성공 후 토큰 DB 저장 중 오류 발생", e);
                                    throw new RuntimeException("DB Save Fail after Login", e);
                                }
                            })
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()) // 블로킹 작업 스레드 풀 할당
                            .thenReturn(authResponse); // 최종적으로 authResponse 반환
                });
    }

    /**
     * POST /api/v1/auth/refresh
     */
    public Mono<AutoDto.AuthResponse> refreshToken(AutoDto.RefreshOrLogoutRequest request, GtrToken existingToken) {
        return webClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("[Refresh Fail] Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("토큰 갱신 실패 (" + clientResponse.statusCode() + "): " + errorBody));
                                })
                )
                .bodyToMono(AutoDto.AuthResponse.class)
                .flatMap(authResponse -> {
                    return Mono.fromRunnable(() -> {
                                try {
                                    String newRefreshToken = authResponse.getRefreshToken();
                                    String newAccessToken = authResponse.getAccessToken();
                                    log.info("✅ [Refresh Success] New Token: {}...", newRefreshToken.substring(0, 5));

                                    Domain domain = new Domain();
                                    domain.setId(DOMAIN_ID);
                                    domain.setName(DOMAIN_NAME);
                                    Domain.setCurrentDomain(domain);

                                    existingToken.setRefreshToken(newRefreshToken);
                                    existingToken.setAccessToken(newAccessToken);

                                    this.queryManager.update(existingToken, "refreshToken", "accessToken");
                                    log.info("✅ DB Update Success (ID: {})", existingToken.getId());

                                } catch (Exception e) {
                                    log.error("토큰 DB 업데이트 중 오류 발생", e);
                                }
                            })
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()) // 블로킹 작업 격리
                            .thenReturn(authResponse);
                });
    }

    /**
     * POST /api/v1/auth/logout
     */
    public Mono<String> logout(AutoDto.RefreshOrLogoutRequest request) {
        return webClient.post()
                .uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(msg -> log.info("✅ [Logout Success] {}", msg))
                .onErrorResume(e -> {
                    log.error("[Logout Fail]", e);
                    return Mono.just("Logout failed but ignored.");
                });
    }

    public void refreshAndSaveToken() {

        Mono.fromCallable(() -> tokenService.getToken())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token -> {

                    if (token == null || token.getRefreshToken() == null) {
                        log.error("❌ [Local Refresh] 저장된 리프레시 토큰이 없습니다.");
                        return Mono.error(new IllegalArgumentException("No refresh token found"));
                    }

                    // 3. 기존 컨트롤러가 하던 요청 파라미터 조립
                    AutoDto.RefreshOrLogoutRequest fixedRequest = new AutoDto.RefreshOrLogoutRequest();
                    fixedRequest.setRefreshToken(token.getRefreshToken());

                    log.info("🔄 [Local Refresh] 조립 완료. 기존 refreshToken 로직을 호출합니다.");

                    // 4. 최종적으로 기존 authService의 핵심 로직 호출
                    return refreshToken(fixedRequest, token);
                })
                // 리턴 타입이 void이므로 여기서 직접 subscribe()를 호출하여 실행시킵니다.
                .subscribe(
                        // 성공했을 때 처리 (필요시 로그 출력)
                        response -> {
                            String accToken = response.getAccessToken();
                            String maskedToken = (accToken != null && accToken.length() >= 5) ? accToken.substring(0, 5) : "null";
                            log.info("🎉 [Local Refresh] 백그라운드 토큰 갱신 완료! (New AccessToken: {}...)", maskedToken);
                        },
                        // 에러가 났을 때 처리
                        error -> log.error("🚨 [Local Refresh] 백그라운드 토큰 갱신 실패: ", error)
                );
    }
}