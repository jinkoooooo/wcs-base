package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.consts.OrderKind;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.event.WcsTaskEvent;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TestService extends AbstractQueryService {

    private final WcsStockAutoService wcsStockAutoService;
    private final EventPublisher eventPublisher;

    public void inspectAllCells() {
        String stockSql = "select * from wcs_stock_info where stock_locked = 0 and stock_disabled = 0";
        List<WcsStockInfo> stockInfoList = this.queryManager.selectListBySql(stockSql, null, WcsStockInfo.class, 0, 0);
        for (WcsStockInfo stockInfo : stockInfoList) {
            WcsStockAuto rack = wcsStockAutoService.getRackByStockId(stockInfo.getStockId());

            String targetSql = """
                SELECT
                    t.*
                FROM
                    wcs_stock_auto t
                JOIN
                    wcs_stock_auto c
                ON
                    c.loc_cd = :targetLocCd
                WHERE
                    t.crane_no = c.crane_no AND
                    t.loc_row >= c.loc_row AND
                    (t.loc_col > c.loc_col OR t.loc_row > c.loc_row) AND
                    t.rack_disabled = 0 AND
                    c.rack_disabled = 0 AND
                    (t.crane_no = 202 OR t.crane_no = 204)
                ORDER BY
                	t.loc_row ASC,
                    t.loc_col ASC
                LIMIT 1
                """;
            Map<String, Object> param = ValueUtil.newMap("targetLocCd", rack.getLocCd());
            WcsStockAuto target = this.queryManager.selectBySql(targetSql, param, WcsStockAuto.class);

            if (ValueUtil.isEmpty(target)) {
                continue;
            }

            WcsTask task = new WcsTask();
            task.setOrderKind(OrderKind.TRANSFER.value());
            task.setCustId(stockInfo.getItemOwner());
            task.setStockId(stockInfo.getStockId());
            task.setItemCode(stockInfo.getItemCode());
            task.setItemName(stockInfo.getItemName());
            task.setStartPointCd(rack.getLocCd());
            task.setEndPointCd(target.getLocCd());
            task.setPlanQty(stockInfo.getBoxQty());

            WcsTaskEvent orderEvent = new WcsTaskEvent();
            orderEvent.setWcsTask(task);
            orderEvent.setOrderKind(OrderKind.TRANSFER.value());
            orderEvent.setMethod("start");
            eventPublisher.publishEvent(orderEvent);
        }
    }
}