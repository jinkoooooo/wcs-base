package operato.logis.wcs.service.impl.allocation.location;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.inventory.service.MultiDeepSortService;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;

/**
 * 방해물 재배치 계획 산출 - Inbound/Outbound/Move Allocator 공통.
 * MultiDeepSortService 호출 및 예외 정책을 단일화한다.
 */
@Component
@RequiredArgsConstructor
public class ObstacleResolver {

    private static final Logger logger = LoggerFactory.getLogger(ObstacleResolver.class);

    private final MultiDeepSortService multiDeepSortService;
    private final InventoryLocationRepository locationRepository;

    /**
     * 대상 로케이션 기준 방해물 재배치 계획을 산출한다.
     * 방해물이 없으면 빈 리스트, 산출 실패 시 예외 throw.
     */
    public List<RelocationTaskDto> resolve(String orderType, String eqGroupId, String targetLocId) {
        try {

            // 대상 로케이션 조회
            ExtTbInventoryLocation targetLocation =
                    locationRepository.findByEqGroupIdAndLocId(eqGroupId, targetLocId);
            if (ValueUtil.isEmpty(targetLocation)) {
                throw new ElidomRuntimeException("로케이션이 존재하지 않습니다.");
            }

            // 방해물 추출 계획 생성
            List<RelocationTaskDto> plan = multiDeepSortService.createRetrievalPlan(targetLocation.getLocCode());

            // 방해물 없으면 빈 리스트 반환
            if (ValueUtil.isEmpty(plan)) {
                logger.info("[ Allocation ][ Loc ] obstacle none - orderType={}, targetLocId={}, targetLocCode={}",
                        orderType, targetLocId, targetLocation.getLocCode());
                return Collections.emptyList();
            }

            logger.info("[ Allocation ][ Loc ] obstacle plan resolved - orderType={}, targetLocId={}, count={}",
                    orderType, targetLocId, plan.size());
            return plan;

        } catch (Exception e) {
            logger.error("[ Allocation ][ Loc ] obstacle resolve failed - orderType={}, targetLocId={}",
                    orderType, targetLocId, e);
            throw new ElidomRuntimeException(
                    WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    String.format("방해물 계획 산출 실패: orderType=%s, targetLocId=%s, error=%s",
                            orderType, targetLocId, e.getMessage()));
        }
    }
}
