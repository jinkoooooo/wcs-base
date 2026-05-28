package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.consts.StockLocked;
import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.service.impl.MultiDatabaseJobService;
import operato.logis.changwon.service.impl.WCS.HIST.WcsStockInfoHistService;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WcsStockInfoService extends AbstractQueryService {

    private final WcsStockInfoHistService wcsStockInfoHistService;
    private final MultiDatabaseJobService multiDatabaseJobService;

    public void createStock(WcsTask task) {
        WcsStockInfo stock = new WcsStockInfo();
        stock.setLcId(WcsConstants.LC_ID);
        stock.setStockId(task.getStockId());
        stock.setItemCode(task.getItemCode());
        stock.setItemName(task.getItemName());
        stock.setItemOwner(task.getCustId());
        stock.setBoxQty(task.getPlanQty());
        stock.setSubStandard(0);
        stock.setSuspended(0);
        stock.setStockLocked(StockLocked.IDLE.value());
        stock.setStockDisabled(0);
        stock.setInbDate(new Date());
        stock.setStockPriority(0);
        stock.setStockType(task.getStockType());
        this.queryManager.insert(stock);

        wcsStockInfoHistService.createHist(stock);
        multiDatabaseJobService.syncWcsToLmsStock(stock);
    }

    public WcsStockInfo getStock(String stockId) {
        String sql = "select * from wcs_stock_info where stock_id=:stockId for update";
        Map<String, Object> param = ValueUtil.newMap("stockId", stockId);
        return this.queryManager.selectBySql(sql, param, WcsStockInfo.class);
    }

    public void deleteStock(String stockId) {
        WcsStockInfo stock = getStock(stockId);
        if (ValueUtil.isNotEmpty(stock)) {
            this.queryManager.delete(stock);

            stock.setBoxQty(0);
            wcsStockInfoHistService.createHist(stock);
        }
    }

    public void lockStock(String stockId, StockLocked locked) {
        WcsStockInfo stock = getStock(stockId);
        if (ValueUtil.isNotEmpty(stock)) {
            stock.setStockLocked(locked.value());
            this.queryManager.update(stock, "stockLocked");

            wcsStockInfoHistService.createHist(stock);
        }
    }

    public void unlockStock(String stockId) {
        WcsStockInfo stock = getStock(stockId);
        if (ValueUtil.isNotEmpty(stock)) {
            stock.setStockLocked(StockLocked.IDLE.value());
            this.queryManager.update(stock, "stockLocked");

            wcsStockInfoHistService.createHist(stock);
        }
    }

    public void completeForceInbound(WcsTask task) {
        WcsStockInfo stockInfo = getStock(task.getStockId());
        if (ValueUtil.isEmpty(stockInfo)) {
            createStock(task);
        } else {
            unlockStock(task.getStockId());
        }
    }
}