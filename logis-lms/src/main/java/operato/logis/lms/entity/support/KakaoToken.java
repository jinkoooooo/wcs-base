package operato.logis.lms.entity.support;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Data
@Table(name = "kakao_token", idStrategy = GenerationRule.UUID)
public class KakaoToken extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @JsonProperty("access_token")
    @Column(name = "access_token", length = 200)
    private String accessToken;

    @JsonProperty("refresh_token")
    @Column(name = "refresh_token",   length = 200)
    private String refreshToken;

    @JsonProperty("expires_in")
    @Column(name = "expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token_expires_in")
    @Column(name = "refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "creator_id",   length = 20)
    private String creatorId;

    @Column(name = "updater_id",   length = 20)
    private String updaterId;

}