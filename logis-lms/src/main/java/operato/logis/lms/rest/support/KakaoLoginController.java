package operato.logis.lms.rest.support;

import operato.logis.lms.entity.support.KakaoToken;
import operato.logis.lms.service.impl.support.KakaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import xyz.elidom.sys.entity.Domain;

import java.util.Date;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/kakao") // 기본 경로 설정
public class KakaoLoginController {

    @Value("${kakao.client-id}")
    private String clientId;

    private final RestTemplate restTemplate;
    private final KakaoService kakaoService;

    private static final String REDIRECT_URI = "https://dx-anyware.logisall.com/rest/kakao/callback";

    private static final String KAUTH_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAUTH_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";

    // 로그인 시작 주소: http://localhost:9500/rest/kakao/login
    @GetMapping("/login")
    public RedirectView login() {
        String url = KAUTH_AUTHORIZE_URL +
                "?client_id=" + clientId +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=code" +
                "&scope=talk_message,friends";

        return new RedirectView(url);
    }

    // 콜백 주소: http://localhost:9500/rest/kakao/callback // 추후 도메인으로 변경
    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code) {
        try {
            Domain newDomain = new Domain("7");

            Domain.setCurrentDomain(newDomain);
            // A. 인증 코드로 토큰 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("redirect_uri", REDIRECT_URI); // 위에서 수정한 URI 사용
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<KakaoToken> response = restTemplate.postForEntity(
                    KAUTH_TOKEN_URL,
                    request,
                    KakaoToken.class
            );

            KakaoToken newToken = response.getBody();

            if (newToken != null) {
                // B. DB 업데이트
                KakaoToken dbToken = kakaoService.getToken();

                if (dbToken == null) {
                    return "실패: DB(kakao_token) 테이블이 비어있습니다. INSERT 문으로 초기 데이터 1건을 넣어주세요.";
                }

                dbToken.setAccessToken(newToken.getAccessToken());
                dbToken.setRefreshToken(newToken.getRefreshToken());
                dbToken.setExpiresIn(newToken.getExpiresIn());
                dbToken.setRefreshTokenExpiresIn(newToken.getRefreshTokenExpiresIn());
                dbToken.setUpdatedAt(new Date());

                // C. 저장
                kakaoService.updateToken(dbToken);

                return "성공! 토큰 발급 완료. 창을 닫으셔도 됩니다.";
            }

        } catch (Exception e) {
            log.error("초기 토큰 발급 실패", e);
            return "에러 발생: " + e.getMessage();
        }

        return "토큰 발급 실패";
    }
}