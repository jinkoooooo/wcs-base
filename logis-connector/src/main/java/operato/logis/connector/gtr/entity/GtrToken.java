package operato.logis.connector.gtr.entity;


import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_gtr_token", idStrategy = GenerationRule.UUID)
public class GtrToken extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "access_token", length = 60)
    private String accessToken;

    @Column(name = "refresh_token", length = 60)
    private String refreshToken;

    @Column(name = "reg_dt")
    private Date regDt;

    @Column(name = "reg_no", length = 16)
    private String regNo;

    @Column(name = "upd_dt")
    private Date updDt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "creator_id",   length = 20)
    private String creatorId;

    @Column(name = "updater_id",   length = 20)
    private String updaterId;

    @Column(name = "created_at")
    private Date createdAt;
}
