package operato.logis.wcs.service.repository;

import operato.logis.wcs.consts.QcTestRequestStatus;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * TbWcsQcTestRequest 영속성 전담 DAO.
 *
 * QC 시험 의뢰 마스터의 조회·생성·수정·삭제를 한 aggregate 단위로 캡슐화한다.
 * 공개 API 는 LocalDate 그대로 받고, 내부에서 LocalDateUtils 로 java.util.Date 변환한다.
 * 엔티티 inboundDate 필드는 ORM 한계로 java.util.Date 로 보관.
 */
@Repository
public class QcTestRequestRepository extends AbstractQueryService {

    /**
     * PK 로 단건 조회.
     */
    public TbWcsQcTestRequest findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(TbWcsQcTestRequest.class, id);
    }

    /**
     * (입고일자, item_code, lot_no) 단건 조회. lot_no 가 비면 "" 로 통일.
     */
    public TbWcsQcTestRequest findByDateAndItemAndLot(LocalDate inboundDate, String itemCode, String lotNo) {
        if (ValueUtil.isEmpty(inboundDate) || ValueUtil.isEmpty(itemCode)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("inbound_date", LocalDateUtils.toDate(inboundDate));
        condition.addFilter("item_code", itemCode);
        condition.addFilter("lot_no", normalizeLot(lotNo));
        return this.queryManager.selectByCondition(TbWcsQcTestRequest.class, condition);
    }

    /**
     * test_request_no 로 단건 조회.
     */
    public TbWcsQcTestRequest findByTestRequestNo(String testRequestNo) {
        if (ValueUtil.isEmpty(testRequestNo)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("test_request_no", testRequestNo);
        return this.queryManager.selectByCondition(TbWcsQcTestRequest.class, condition);
    }

    /**
     * 입고일자로 의뢰 목록 조회 (item_code, lot_no 순).
     */
    public List<TbWcsQcTestRequest> findByInboundDate(LocalDate inboundDate) {
        if (ValueUtil.isEmpty(inboundDate)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("inbound_date", LocalDateUtils.toDate(inboundDate));
        condition.addOrder("item_code", true);
        condition.addOrder("lot_no", true);
        return this.queryManager.selectList(TbWcsQcTestRequest.class, condition);
    }

    /**
     * 상위 미인수(fetched=false) 의뢰 목록 조회 (생성순). pull 대상.
     */
    public List<TbWcsQcTestRequest> findUnfetched() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("fetched", Boolean.FALSE);
        condition.addOrder("created_at", true);
        return this.queryManager.selectList(TbWcsQcTestRequest.class, condition);
    }

    /**
     * status 로 의뢰 목록 조회 (최신순).
     */
    public List<TbWcsQcTestRequest> findByStatus(QcTestRequestStatus status) {
        if (ValueUtil.isEmpty(status)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("status", status.code());
        condition.addOrder("created_at", false);
        return this.queryManager.selectList(TbWcsQcTestRequest.class, condition);
    }

    /**
     * 신규 의뢰 insert.
     */
    public TbWcsQcTestRequest insert(TbWcsQcTestRequest entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 전체 또는 지정 컬럼만 update.
     */
    public TbWcsQcTestRequest update(TbWcsQcTestRequest entity, String... columns) {
        if (ValueUtil.isEmpty(columns)) {
            this.queryManager.update(entity);
        } else {
            this.queryManager.update(entity, columns);
        }
        return entity;
    }

    /**
     * 의뢰 삭제.
     */
    public void delete(TbWcsQcTestRequest entity) {
        if (ValueUtil.isEmpty(entity)) return;
        this.queryManager.delete(entity);
    }

    /**
     * lot_no NULL/공백은 빈 문자열로 정규화하여 UNIQUE 키 안정성 확보.
     */
    private static String normalizeLot(String lotNo) {
        return ValueUtil.isEmpty(lotNo) ? "" : lotNo;
    }

    /**
     * test_request_no 로 의뢰 목록 조회 (정책상 단건이지만 방어적으로 리스트).
     */
    public List<TbWcsQcTestRequest> findAllByTestRequestNo(String testRequestNo) {
        if (ValueUtil.isEmpty(testRequestNo)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("test_request_no", testRequestNo);
        return this.queryManager.selectList(TbWcsQcTestRequest.class, condition);

    }
}
