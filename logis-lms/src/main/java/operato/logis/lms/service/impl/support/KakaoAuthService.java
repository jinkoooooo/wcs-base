package operato.logis.lms.service.impl.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.lms.entity.support.KakaoToken; // 하나만 import 하면 됨
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import xyz.elidom.sys.entity.Domain;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    private final RestTemplate restTemplate;
    private final KakaoService kakaoService;


    private static final String KAUTH_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    public void refreshAccessToken() {
        KakaoToken currentToken = kakaoService.getToken();

        Domain newDomain = new Domain("7");

        Domain.setCurrentDomain(newDomain);

        if (currentToken == null) {
            log.error("❌ DB에 토큰 정보가 없습니다.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("refresh_token", currentToken.getRefreshToken()); // DB의 리프레시 토큰 사용

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 3. API 호출: 응답을 KakaoToken 클래스로 바로 받음 (DTO 없이!)
            ResponseEntity<KakaoToken> response = restTemplate.postForEntity(
                    KAUTH_TOKEN_URL,
                    request,
                    KakaoToken.class
            );

            KakaoToken receivedToken = response.getBody();

            if (receivedToken != null) {
                currentToken.setAccessToken(receivedToken.getAccessToken());
                currentToken.setExpiresIn(receivedToken.getExpiresIn());

                if (receivedToken.getRefreshToken() != null) {
                    currentToken.setRefreshToken(receivedToken.getRefreshToken());
                    currentToken.setRefreshTokenExpiresIn(receivedToken.getRefreshTokenExpiresIn());
                }

                currentToken.setUpdatedAt(new Date());

                kakaoService.updateToken(currentToken);

                log.info("✅ 카카오 토큰 갱신 완료");
            }

        } catch (Exception e) {
            log.error("❌ 토큰 갱신 실패: ", e);
        }
    }

    public void sendToFriends(String title, String centerCode, String requester,
                              String category, String equipId, String alarmId, String content) {
        try {
            String accessToken = kakaoService.getToken().getAccessToken();
            RestTemplate rt = new RestTemplate();

            // 1. 친구 목록 조회
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = rt.exchange(
                    "https://kapi.kakao.com/v1/api/talk/friends", HttpMethod.GET, entity, String.class
            );

            String body = response.getBody();
            if (body == null) return;

            // 2. UUID 추출
            Pattern pattern = Pattern.compile("\"uuid\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(body);

            StringBuilder uuids = new StringBuilder("[");
            boolean hasFriend = false;
            while (matcher.find()) {
                if (uuids.length() > 1) uuids.append(",");
                uuids.append("\"").append(matcher.group(1)).append("\"");
                hasFriend = true;
            }
            uuids.append("]");

            if (!hasFriend) {
                log.info("⚠️ 보낼 친구가 없습니다.");
                return;
            }

            String safeTitle = cleanText(title);
            String safeContent = cleanText(content);
            if (safeContent.isEmpty()) safeContent = "상세 내용 없음";
            // 내용이 너무 길면 짤라서 보여줌
            if (safeContent.length() > 100) safeContent = safeContent.substring(0, 97) + "...";

            String safeCategory = cleanText(category).isEmpty() ? "기타" : cleanText(category);
            String safeCenter = cleanText(centerCode);
            String safeEquip = cleanText(equipId).isEmpty() ? "-" : cleanText(equipId);
            String safeAlarm = cleanText(alarmId).isEmpty() ? "-" : cleanText(alarmId);
            String safeRequester = cleanText(requester);

            String imageUrl = "https://winus.logisall.com/ATCH_FILE/STAN_TEMPLATE/LWINUS_ANYWARE/ANYWARE.jpg";

            String templateObject = "{"
                    + "\"object_type\": \"feed\","
                    + "\"content\": {"
                    + "    \"title\": \"[유지보수 요청] " + safeTitle + "\","
                    + "    \"description\": \"" + safeContent + "\","  // 여기에 상세 내용을 넣습니다
                    + "    \"image_url\": \"" + imageUrl + "\","
                    + "    \"image_width\": 640,"
                    + "    \"image_height\": 640,"
                    + "    \"link\": {"
                    + "        \"web_url\": \"https://dx-anyware.logisall.com/\","
                    + "        \"mobile_web_url\": \"https://dx-anyware.logisall.com/\""
                    + "    }"
                    + "},"
                    + "\"item_content\": {"
                    + "    \"profile_text\": \"Anyware AutoStore 알림\","
                    + "    \"items\": ["
                    + "        {\"item\": \"분류\", \"item_op\": \"" + safeCategory + "\"},"
                    + "        {\"item\": \"센터\", \"item_op\": \"" + safeCenter + "\"},"
                    + "        {\"item\": \"설비\", \"item_op\": \"" + safeEquip + "\"},"
                    + "        {\"item\": \"알람\", \"item_op\": \"" + safeAlarm + "\"},"
                    + "        {\"item\": \"요청자\", \"item_op\": \"" + safeRequester + "\"}"
                    + "    ]"
                    + "},"
                    + "\"buttons\": ["
                    + "    {"
                    + "        \"title\": \"시스템 바로가기\","
                    + "        \"link\": {"
                    + "            \"web_url\": \"https://dx-anyware.logisall.com/\","
                    + "            \"mobile_web_url\": \"https://dx-anyware.logisall.com/\""
                    + "        }"
                    + "    }"
                    + "]"
                    + "}";

            // 3. 전송
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("receiver_uuids", uuids.toString());
            params.add("template_object", templateObject);

            HttpEntity<MultiValueMap<String, String>> sendRequest = new HttpEntity<>(params, headers);

            rt.postForObject("https://kapi.kakao.com/v1/api/talk/friends/message/default/send", sendRequest, String.class);

            log.info("✅ 아이템 리스트 메시지 전송 성공!");

        } catch (Exception e) {
            log.error("❌ 전송 실패: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // [필수] 특수문자 청소 헬퍼 메서드 (클래스 안에 같이 있어야 함)
    private String cleanText(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", " ").replace("\r", "").replace("\\", "/").trim();
    }
}