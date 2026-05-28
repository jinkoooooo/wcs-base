package operato.logis.changwon.service.impl.WCS.HIST;

import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsStockAutoHist;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

@Service
public class WcsStockAutoHistService extends AbstractQueryService {

    public void createHist(WcsStockAuto rack) {
        WcsConstants.setupDomainContext();
        WcsStockAutoHist rackHist = convertToHistory(rack);
        if (ValueUtil.isNotEmpty(rackHist)) {
            this.queryManager.insert(rackHist);
        }
    }

    private WcsStockAutoHist convertToHistory(WcsStockAuto rack) {
        if (rack == null) {
            return null;
        }

        WcsStockAutoHist hist = new WcsStockAutoHist();
        hist.setStockType(rack.getStockType());
        hist.setCraneNo(rack.getCraneNo());
        hist.setLocLevel(rack.getLocLevel());
        hist.setLocCol(rack.getLocCol());
        hist.setLocRow(rack.getLocRow());
        hist.setLocDeep(rack.getLocDeep());
        hist.setLocSide(rack.getLocSide());
        hist.setLocCd(rack.getLocCd());
        hist.setRackLocked(rack.getRackLocked());
        hist.setRackDisabled(rack.getRackDisabled());
        hist.setStockId(rack.getStockId());
        hist.setTaskId(rack.getTaskId());
        hist.setStoreDate(rack.getStoreDate());
        hist.setShipDate(rack.getShipDate());
        hist.setCraneDisabled(rack.getCraneDisabled());
        hist.setRackHeight(rack.getRackHeight());
        hist.setRackWeight(rack.getRackWeight());
        hist.setStockGrade(rack.getStockGrade());
        hist.setDedicatedType(rack.getDedicatedType());
        hist.setRestrictedType(rack.getRestrictedType());
        hist.setOwnerGroupCode(rack.getOwnerGroupCode());
        hist.setAttributeA(rack.getAttributeA());
        hist.setAttributeB(rack.getAttributeB());
        hist.setPositionX(rack.getPositionX());
        hist.setPositionY(rack.getPositionY());
        hist.setPositionZ(rack.getPositionZ());

        return hist;
    }
}