package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.query.store.LocationQueryStore;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationRecommendationService extends AbstractQueryService {

    private final LocationQueryStore locationQueryStore;

    /**
     * 화주사 그룹 코드에 따른 입고 로케이션 산출
     */
    public WcsStockAuto getInboundLocation(String itemOwner) {
        String sql = locationQueryStore.getRecommendInboundLocationSql();
        Map<String, Object> param = ValueUtil.newMap("itemOwner", itemOwner);
        return this.queryManager.selectBySql(sql, param, WcsStockAuto.class);
    }

    /**
     * 화주사 그룹 코드에 따른 정렬 로케이션 산출
     */
    public List<WcsStockAuto> getSortLocationList(WcsStockAuto rack) {
        String sql = locationQueryStore.getRecommendSortLocationSql();
        Map<String, Object> param = ValueUtil.newMap("craneNo,locRow,ownerGroupCode,locSide", rack.getCraneNo(), rack.getLocRow(), rack.getOwnerGroupCode(), rack.getLocSide());
        return this.queryManager.selectListBySql(sql, param, WcsStockAuto.class, 0, 0);
    }
}