package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipTaskType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipType;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsErrorCode;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsException;
import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

/**
 * ============================================================================
 * Processor Support Service
 * ============================================================================
 *
 * [역할]
 * - 여러 processor 에서 공통으로 사용하는 기계적 처리 담당
 * - business rule 은 넣지 않는다.
 *
 * [포함 기능]
 * - 위치 조회 / 기본 검증
 * - groupCd 검증
 * - 기본 freight move order 생성
 * - lock rollback
 */
@Service
public class ProcessorSupportService {

    @Autowired
    protected TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected LocationLockService locationLockService;

    /**
     * locationCd 기준 위치 조회
     */
    public TbEcsLocMst getRequiredLocation(String locationCd, String messagePrefix) {
        TbEcsLocMst loc = tbEcsLocMstService.findLocationByCode(locationCd);

        if (ValueUtil.isEmpty(loc)) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    messagePrefix + " 위치를 찾을 수 없습니다. locationCd=" + locationCd
            );
        }

        return loc;
    }

    /**
     * 사용 가능한 기본 위치 검증
     */
    public void validateUsableLocation(String locationCd, TbEcsLocMst loc, String messagePrefix) {
        if (ValueUtil.isEmpty(loc)) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    messagePrefix + " 위치를 찾을 수 없습니다. locationCd=" + locationCd
            );
        }

        if (ValueUtil.isEmpty(loc.getLocationCd())) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    messagePrefix + " 위치 코드가 비어 있습니다. locationCd=" + locationCd
            );
        }

        if (loc.getLocationUseYn() == null || loc.getLocationUseYn() != 1) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    messagePrefix + " 위치가 사용 불가 상태입니다. locationCd=" + locationCd
            );
        }
    }

    /**
     * 특정 groupCd 검증
     */
    public void validateGroupCd(TbEcsLocMst loc, String expectedGroupCd, String messagePrefix) {
        if (!ValueUtil.isEqual(loc.getGroupCd(), expectedGroupCd)) {
            throw new EcsException(
                    EcsErrorCode.INVALID_PARAMETER,
                    messagePrefix + " 위치의 groupCd가 올바르지 않습니다. expected=" + expectedGroupCd + ", actual=" + loc.getGroupCd()
            );
        }
    }

    /**
     * Freight move 성격의 공통 order 생성
     */
    public TbWcsOrder buildFreightMoveOrder(
            String orderId,
            String taskId,
            TbEcsLocMst fromLoc,
            TbEcsLocMst toLoc,
            CommandType commandType
    ) {
        TbWcsOrder order = tbWcsOrderService.createOrder(
                taskId,
                fromLoc.getLocationCd(),
                toLoc.getLocationCd(),
                fromLoc.getPodCd(),
                commandType,
                EquipType.AGF,
                null,
                1
        );

        order.setOrderId(orderId);
        order.setTaskType(EquipTaskType.FREIGHT_MOVE.getCode());

        return order;
    }

    /**
     * to lock 해제
     */
    public void rollbackToLock(String orderId, TbEcsLocMst toLoc) {
        if (ValueUtil.isNotEmpty(toLoc) && ValueUtil.isNotEmpty(toLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(toLoc.getLocationCd(), orderId);
        }
    }

    /**
     * from/to lock 모두 해제
     */
    public void rollbackBothLocks(String orderId, TbEcsLocMst fromLoc, TbEcsLocMst toLoc) {
        if (ValueUtil.isNotEmpty(fromLoc) && ValueUtil.isNotEmpty(fromLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(fromLoc.getLocationCd(), orderId);
        }

        if (ValueUtil.isNotEmpty(toLoc) && ValueUtil.isNotEmpty(toLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(toLoc.getLocationCd(), orderId);
        }
    }
}