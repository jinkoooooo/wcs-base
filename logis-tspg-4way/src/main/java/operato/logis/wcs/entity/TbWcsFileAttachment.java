package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 공통 파일 첨부 마스터.
 *
 * 모든 화면에서 파일은 본 테이블에 저장하고, 비즈 엔티티는 id 만 참조한다.
 * - 화면별 컨트롤러/스토리지 분기 불필요
 * - 같은 파일을 여러 엔티티가 참조해도 무방
 * - category 로 업로드 도메인 구분 (qc_test, inbound, return ...)
 */
@Getter
@Setter
@Table(name = "tb_wcs_file_attachment", idStrategy = GenerationRule.UUID)
public class TbWcsFileAttachment extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    /** 업로드 도메인 (qc_test / inbound / return ...). 로깅·정리 용도. */
    @Column(name = "category", length = 50)
    private String category;

    /** 원본 파일명. */
    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    /** 서버 저장 절대경로. */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** 파일 크기 (bytes). */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** MIME content-type (application/pdf 등). */
    @Column(name = "content_type", length = 100)
    private String contentType;
}