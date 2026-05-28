package operato.logis.wcs.service.repository;

import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Filters;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * TbWcsHostOrder 영속성 전담 DAO.
 *
 * 호스트 주문의 조회·생성·상태/에러 갱신·재시도 및 스케줄 대상 조회를 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class HostOrderRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public TbWcsHostOrder findById(String id) {
        return this.queryManager.select(TbWcsHostOrder.class, id);
    }

    /** host_order_key 로 단건 조회. */
    public TbWcsHostOrder findByHostOrderKey(String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /** (host_system_code, host_order_key) 로 단건 조회. */
    public TbWcsHostOrder findByHostOrderKey(String hostSystemCode, String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /** wcs_order_key 로 단건 조회. */
    public TbWcsHostOrder findByWcsOrderKey(String wcsOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("wcs_order_key", wcsOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /** order_status 로 목록 조회. */
    public List<TbWcsHostOrder> findByStatus(int orderStatus) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", orderStatus);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }

    /** 신규 주문 insert. */
    public TbWcsHostOrder insert(TbWcsHostOrder entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 지정 필드 제외 update. */
    public TbWcsHostOrder update(TbWcsHostOrder entity, String... excludeFields) {
        this.queryManager.update(entity, excludeFields);
        return entity;
    }

    /** 재시도 대상 조회 — ERROR 이상이면서 wcs_order_key 가 비었거나 NULL 인 주문 (생성순). */
    public List<TbWcsHostOrder> findOrdersForRetry() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", OrmConstants.GREATER_THAN_EQUAL, HostOrderStatus.ERROR.code());

        // wcs_order_key 가 "" 이거나 NULL 인 것 (OR 조건)
        Filters wcsKeyOrGroup = new Filters(Filters.OPERATOR_OR);
        wcsKeyOrGroup.addFilter("wcs_order_key", "");
        wcsKeyOrGroup.addFilter("wcs_order_key", OrmConstants.IS_NULL, null);
        condition.addFilters(wcsKeyOrGroup);

        condition.addOrder("createdAt", true);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }

    /** parent_host_order_key 로 자식 주문 조회 (status 필터 옵션, 생성순). */
    public List<TbWcsHostOrder> findByParentHostOrderKey(String parentKey, Integer statusFilter) {
        if (ValueUtil.isEmpty(parentKey)) return Collections.emptyList();
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("parent_host_order_key", parentKey);
        // statusFilter 지정 시에만 상태 필터 추가
        if (ValueUtil.isNotEmpty(statusFilter)) {
            condition.addFilter("order_status", statusFilter);
        }
        condition.addOrder("createdAt", true);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }

    /** 예약일이 today 이하인 WAITING_SCHEDULE 주문 조회 (예약일·우선순위 순). */
    public List<TbWcsHostOrder> findWaitingScheduleDueBy(LocalDate today) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", HostOrderStatus.WAITING_SCHEDULE.code());
        condition.addFilter("scheduled_date", OrmConstants.LESS_THAN_EQUAL, LocalDateUtils.toDate(today));
        condition.addOrder("scheduledDate", true);
        condition.addOrder("priority", true);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }

    /** 할당 준비된(READY_FOR_ALLOC) 주문 조회 (우선순위·생성순). */
    public List<TbWcsHostOrder> findReadyForAllocation() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", HostOrderStatus.READY_FOR_ALLOC.code());
        condition.addOrder("priority", true);
        condition.addOrder("createdAt", true);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }
}
