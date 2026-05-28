package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsFileAttachment;
import org.springframework.stereotype.Repository;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * TbWcsFileAttachment 영속성 전담 DAO.
 *
 * 첨부파일 메타데이터의 조회·생성·수정·삭제를 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class FileAttachmentRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public TbWcsFileAttachment findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(TbWcsFileAttachment.class, id);
    }

    /** 신규 첨부 insert. */
    public TbWcsFileAttachment insert(TbWcsFileAttachment entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 전체 또는 지정 컬럼만 update. */
    public TbWcsFileAttachment update(TbWcsFileAttachment entity, String... columns) {
        if (ValueUtil.isEmpty(columns)) {
            this.queryManager.update(entity);
        } else {
            this.queryManager.update(entity, columns);
        }
        return entity;
    }

    /** 첨부 삭제. */
    public void delete(TbWcsFileAttachment entity) {
        if (ValueUtil.isEmpty(entity)) return;
        this.queryManager.delete(entity);
    }
}
