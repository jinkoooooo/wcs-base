package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsSystemMode;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * TbWcsSystemMode 영속성 전담 DAO.
 *
 * 시스템 운영 모드(전역·그룹별)의 조회·upsert·삭제를 한 aggregate 단위로 캡슐화한다.
 * GLOBAL_ID 는 그룹 미지정 전역 모드의 고정 PK.
 */
@Repository
public class SystemModeRepository extends AbstractQueryService {

    public static final String GLOBAL_ID = "GLOBAL";

    /** PK 로 단건 조회. */
    public TbWcsSystemMode findById(String id) {
        return this.queryManager.select(TbWcsSystemMode.class, ValueUtil.newMap("id", id));
    }

    /** eqGroup 으로 모드 조회. */
    public TbWcsSystemMode findByEqGroupId(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        return this.queryManager.selectByCondition(TbWcsSystemMode.class, condition);
    }

    /** 전역 모드(GLOBAL_ID) 조회. */
    public TbWcsSystemMode findGlobal() {
        return this.findById(GLOBAL_ID);
    }

    /** 신규 모드 insert. */
    public TbWcsSystemMode insert(TbWcsSystemMode entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 지정 필드 제외 update. */
    public TbWcsSystemMode update(TbWcsSystemMode entity, String... excludeFields) {
        this.queryManager.update(entity, excludeFields);
        return entity;
    }

    /** 전체 update. */
    public TbWcsSystemMode update(TbWcsSystemMode entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** PK 존재 여부에 따라 insert 또는 update. */
    public TbWcsSystemMode upsert(TbWcsSystemMode entity) {
        TbWcsSystemMode existing = this.findById(entity.getId());
        // 기존 없으면 insert, 있으면 update
        if (ValueUtil.isEmpty(existing)) {
            return this.insert(entity);
        }
        return this.update(entity);
    }

    /** PK 로 삭제 (존재할 때만). */
    public void delete(String id) {
        TbWcsSystemMode entity = this.findById(id);
        if (ValueUtil.isNotEmpty(entity)) {
            this.queryManager.delete(entity);
        }
    }
}
