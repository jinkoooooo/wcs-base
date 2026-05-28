package operato.logis.asrs.core.stock;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.request.OutboundLocation2DExecuteRequest;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 2D 로케이션 지정출고 실행 Core.
 *
 * 현재 정책:
 * - 선택한 stockUnitNo 기준 전체출고 처리
 * - 실제 운영 정책에 맞춰 추후 partial out / allocation 연계 가능
 */
@Service
@RequiredArgsConstructor
public class OutboundLocation2DCommandCore extends AbstractQueryService {

    /**
     * 지정출고 실행
     *
     * 주의:
     * - 현재는 기존 재고 출고 command 와 동일하게 stock transaction insert / stock status update 로 연결해야 함
     * - 프로젝트 내 기존 full outbound 처리 core 가 있다면 그 core 를 호출하는 방식으로 대체 권장
     */
    public void executeOutbound(OutboundLocation2DExecuteRequest request) {
        if (request == null || request.getStockUnitNo() == null || request.getStockUnitNo().trim().isEmpty()) {
            throw new IllegalArgumentException("stockUnitNo is required.");
        }

        /**
         * TODO:
         * - 현재 프로젝트의 기존 전체출고 core / service 호출로 연결
         * - 아래는 예시 placeholder
         */
        String sql = ""
                + "update tb_ac_stock \n"
                + "set stock_status_code = 'OUTBOUND', \n"
                + "    qty = 0, \n"
                + "    reserved_qty = 0, \n"
                + "    updated_at = now() \n"
                + "where stock_unit_no = :stockUnitNo";

        Map<String, Object> params = ValueUtil.newMap(
                "stockUnitNo", request.getStockUnitNo()
        );

        this.queryManager.executeBySql(sql, params);

        /**
         * TODO:
         * - tb_ac_stock_txn 이력 insert
         * - refDocType / refDocNo / reasonCode / remark 기록
         */
    }
}