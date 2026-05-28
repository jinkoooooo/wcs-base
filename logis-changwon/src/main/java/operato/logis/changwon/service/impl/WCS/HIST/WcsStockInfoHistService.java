package operato.logis.changwon.service.impl.WCS.HIST;

import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsStockInfoHist;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

@Service
public class WcsStockInfoHistService extends AbstractQueryService {

    public void createHist(WcsStockInfo stockInfo) {
        WcsStockInfoHist stockHist = convertToHist(stockInfo);
        if (ValueUtil.isNotEmpty(stockHist)) {
            this.queryManager.insert(stockHist);
        }
    }

    private WcsStockInfoHist convertToHist(WcsStockInfo stockInfo) {
        if (stockInfo == null) {
            return null;
        }

        WcsStockInfoHist hist = new WcsStockInfoHist();
        hist.setLcId(stockInfo.getLcId());
        hist.setStockId(stockInfo.getStockId());
        hist.setSkuId(stockInfo.getSkuId());
        hist.setItemCode(stockInfo.getItemCode());
        hist.setItemName(stockInfo.getItemName());
        hist.setItemOwner(stockInfo.getItemOwner());
        hist.setBoxQty(stockInfo.getBoxQty());
        hist.setSubStandard(stockInfo.getSubStandard());
        hist.setSuspended(stockInfo.getSuspended());
        hist.setStockLocked(stockInfo.getStockLocked());
        hist.setStockDisabled(stockInfo.getStockDisabled());
        hist.setInbDate(stockInfo.getInbDate());
        hist.setLotNo(stockInfo.getLotNo());
        hist.setInbDelNo(stockInfo.getInbDelNo());
        hist.setStockPriority(stockInfo.getStockPriority());
        hist.setStockMemo(stockInfo.getStockMemo());
        hist.setAttributeA(stockInfo.getAttributeA());
        hist.setAttributeB(stockInfo.getAttributeB());
        hist.setAttributeC(stockInfo.getAttributeC());

        return hist;
    }
}
