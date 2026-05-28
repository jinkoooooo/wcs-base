package operato.logis.connector.gtr.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "tb_mw_vision_data", idStrategy = GenerationRule.UUID)
public class VisionValue extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    // 2. seqno
    // 컬럼명: seqno, Data type: varchar(10)
    @Column(name = "seqno", length = 10)
    private String seqno;

    // 3. result_code
    // 컬럼명: result_code, Data type: varchar(10)
    @Column(name = "result_code", length = 10)
    private String resultCode;

    // 4. filenametop
    // 컬럼명: filenametop, Data type: varchar(64)
    @Column(name = "filenametop", length = 64)
    private String filenametop;

    // 5. filenamefront
    // 컬럼명: filenamefront, Data type: varchar(64)
    @Column(name = "filenamefront", length = 64)
    private String filenamefront;

    // 6. filenameback
    // 컬럼명: filenameback, Data type: varchar(64)
    @Column(name = "filenameback", length = 64)
    private String filenameback;

    // 7. filenameleft
    // 컬럼명: filenameleft, Data type: varchar(64)
    @Column(name = "filenameleft", length = 64)
    private String filenameleft;

    // 8. filenameright
    // 컬럼명: filenameright, Data type: varchar(64)
    @Column(name = "filenameright", length = 64)
    private String filenameright;

    // 9. filenamebottom
    // 컬럼명: filenamebottom, Data type: varchar(64)
    @Column(name = "filenamebottomleft", length = 64)
    private String filenamebottomleft;
    // 9. filenamebottom
    // 컬럼명: filenamebottom, Data type: varchar(64)
    @Column(name = "filenamebottomright", length = 64)
    private String filenamebottomright;

    // 10. result
    // 컬럼명: result, Data type: varchar(20)
    @Column(name = "result", length = 20)
    private String result;

    // 11. receive_time
    // 컬럼명: receive_time, Data type: varchar(20)
    @Column(name = "receive_time", length = 20)
    private String receiveTime;

    // 12. send_yn
    // 컬럼명: send_yn, Data type: varchar(2), Default: 'N'
    @Column(name = "send_yn", length = 2)
    private String sendYn = "N"; // Default 값을 자바 필드에도 설정할 수 있습니다.

    // 13. reg_dt (등록일시)
    // 컬럼명: reg_dt, Data type: timestamp
    // JPA에서 timestamp는 보통 LocalDateTime 또는 Instant를 사용합니다.
    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    // 14. reg_no
    // 컬럼명: reg_no, Data type: varchar(15)
    @Column(name = "reg_no", length = 15)
    private String regNo;

    // 15. upd_dt (수정일시)
    // 컬럼명: upd_dt, Data type: timestamp
    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    // 16. upd_no
    // 컬럼명: upd_no, Data type: varchar(15)
    @Column(name = "upd_no", length = 15)
    private String updNo;

    private String barcodedata;

}
