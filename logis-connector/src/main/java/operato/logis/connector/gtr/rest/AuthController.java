package operato.logis.connector.gtr.rest;

import operato.logis.connector.gtr.dto.AutoDto;
import operato.logis.connector.gtr.dto.InspectionRequestDto;
import operato.logis.connector.gtr.dto.InspectionResultDto;
import operato.logis.connector.gtr.entity.GtrToken;
import operato.logis.connector.gtr.service.AuthService;
import operato.logis.connector.gtr.service.InspectionService;
import operato.logis.connector.gtr.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/client")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final InspectionService inspectionService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, InspectionService inspectionService, TokenService tokenService) {
        this.authService = authService;
        this.inspectionService = inspectionService;
        this.tokenService = tokenService;
    }

    // --- 인증 API ---

    @PostMapping("/login")
    public Mono<AutoDto.AuthResponse> login() {
        // 테스트용 하드코딩 (실제 운영 시에는 RequestBody로 받는 것을 권장)
        AutoDto.LoginRequest fixedRequest = new AutoDto.LoginRequest();
        fixedRequest.setUsername("hscdc");
        fixedRequest.setPassword("Logis@ll26");

        log.info("👉 [Login Request] User: {}", fixedRequest.getUsername());
        return authService.login(fixedRequest);
    }

    @PostMapping("/refresh")
    public Mono<AutoDto.AuthResponse> refreshToken() {
        AutoDto.RefreshOrLogoutRequest fixedRequest = new AutoDto.RefreshOrLogoutRequest();

        GtrToken token = tokenService.getToken();

        if (token == null || token.getRefreshToken() == null) {
            log.error("❌ 저장된 리프레시 토큰이 없습니다.");
            return Mono.error(new IllegalArgumentException("No refresh token found"));
        }

        fixedRequest.setRefreshToken(token.getRefreshToken());

        return authService.refreshToken(fixedRequest, token);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout(@RequestBody AutoDto.RefreshOrLogoutRequest request) {
        log.info("👉 [Logout Request]");
        return authService.logout(request)
                .map(ResponseEntity::ok);
    }

    // --- 검수 API ---

    @PostMapping("/inspections")
    public ResponseEntity<Object> submitInspection(@RequestBody InspectionRequestDto requestDto) { // 리턴 타입을 Object로 변경하여 성공(DTO)/실패(Map) 모두 수용
        log.info("=== Received Inspection Request ===");
        log.info("TransactionId: {}", requestDto.getTransactionId());

        String siteId = "hwaseong/damage-detection"; // 고정값 or 파라미터화

        try {
            // [수정] Map -> InspectionResultDto
            InspectionResultDto result = inspectionService
                    .requestInspection(siteId, requestDto)
                    .block(); // 결과 대기

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Inspection request failed", e);
            // 에러 시에는 여전히 Map 형태로 메시지를 내려주는 것이 일반적입니다.
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Inspection request failed: " + e.getMessage()));
        }
    }
}