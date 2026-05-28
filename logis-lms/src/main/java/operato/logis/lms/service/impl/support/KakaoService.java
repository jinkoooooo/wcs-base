package operato.logis.lms.service.impl.support;

import operato.logis.lms.entity.support.KakaoToken;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
public class KakaoService extends AbstractQueryService {

    public KakaoToken getToken() {
        String sql = "select * from kakao_token";
        return this.queryManager.selectBySql(sql, null, KakaoToken.class);
    }


    public void updateToken(KakaoToken token) {
        this.queryManager.update(token,
                "accessToken",
                "refreshToken",
                "expiresIn",
                "refreshTokenExpiresIn",
                "updatedAt" // 갱신 시간도 함께 업데이트
        );
    }
}
