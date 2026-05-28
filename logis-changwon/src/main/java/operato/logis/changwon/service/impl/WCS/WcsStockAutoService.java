package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.consts.RackLocked;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.service.impl.WCS.HIST.WcsStockAutoHistService;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WcsStockAutoService extends AbstractQueryService {

    private final WcsStockAutoHistService wcsStockAutoHistService;

    /**
     * 특정 Cell 조회
     */
    public WcsStockAuto getRackByLocCd(String locCd) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("locCd", locCd);
        return this.queryManager.selectByConditionWithLock(WcsStockAuto.class, condition);
    }

    public WcsStockAuto getRackByStockId(String stockId) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("stockId", stockId);
        return this.queryManager.selectByConditionWithLock(WcsStockAuto.class, condition);
    }

    /**
     * 특정 Cell에서 중앙 통로까지 경로 Cell List 조회
     */
    public List<WcsStockAuto> getTaskPath(WcsStockAuto rack) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("craneNo", rack.getCraneNo());
        condition.addFilter("locRow", rack.getLocRow());
        condition.addFilter("locSide", rack.getLocSide());
        condition.addFilter("locDeep", "<", rack.getLocDeep());
        condition.addOrder("locDeep", true);
        return this.queryManager.selectListWithLock(WcsStockAuto.class, condition);
    }

    /**
     * 특정 Cell에서 벽까지 경로 Cell List 조회
     */
    public List<WcsStockAuto> getTaskBack(WcsStockAuto rack) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("craneNo", rack.getCraneNo());
        condition.addFilter("locRow", rack.getLocRow());
        condition.addFilter("locSide", rack.getLocSide());
        condition.addFilter("locDeep", ">", rack.getLocDeep());
        condition.addOrder("locDeep", true);
        return this.queryManager.selectListWithLock(WcsStockAuto.class, condition);
    }

    /**
     * 특정 CraneNo 로케이션 조회
     */
    public List<WcsStockAuto> getListByCraneNo(int craneNo) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("craneNo", craneNo);
        return this.queryManager.selectListWithLock(WcsStockAuto.class, condition);
    }

    /**
     * 특정 Cell에서 중앙 통로까지 경로 잠금
     */
    public void lockPath(WcsStockAuto rack, RackLocked locked, String taskId) {
        List<WcsStockAuto> paths = getTaskPath(rack);
        for (WcsStockAuto path : paths) {
            path.setRackLocked(locked.value());
            path.setTaskId(taskId);
            this.queryManager.update(path, "rackLocked", "taskId");

            wcsStockAutoHistService.createHist(path);
        }
    }

    /**
     * 특정 Cell에서 벽까지 하이랙 잠금
     */
    public void lockBack(WcsStockAuto rack, RackLocked locked, String taskId) {
        List<WcsStockAuto> paths = getTaskBack(rack);
        for (WcsStockAuto path : paths) {
            path.setRackLocked(locked.value());
            path.setTaskId(taskId);
            this.queryManager.update(path, "rackLocked", "taskId");

            wcsStockAutoHistService.createHist(path);
        }
    }

    /**
     * 특정 Cell 잠금
     */
    public void lockRack(WcsStockAuto rack, RackLocked locked, String taskId) {
        rack.setRackLocked(locked.value());
        rack.setTaskId(taskId);
        this.queryManager.update(rack, "rackLocked", "taskId");

        wcsStockAutoHistService.createHist(rack);
    }

    /**
     * 작업 완료 후 Cell 잠금 해제
     */
    public void unlockRack(String taskId) {
        Query condition = OrmUtil.newConditionForExecution(WcsConstants.DOMAIN_ID);
        condition.addFilter("taskId", taskId);
        List<WcsStockAuto> lockedRacks = this.queryManager.selectListWithLock(WcsStockAuto.class, condition);

        for (WcsStockAuto rack : lockedRacks) {
            rack.setRackLocked(RackLocked.IDLE.value());
            rack.setTaskId(null);
            this.queryManager.update(rack, "rackLocked", "taskId");

            wcsStockAutoHistService.createHist(rack);
        }
    }

    public void setStockId(String locCd, String stockId) {
        WcsStockAuto rack = getRackByLocCd(locCd);
        rack.setStockId(stockId);
        this.queryManager.update(rack, "stockId");

        wcsStockAutoHistService.createHist(rack);
    }

    public void resetStockId(String locCd) {
        WcsStockAuto rack = getRackByLocCd(locCd);
        rack.setStockId(null);
        this.queryManager.update(rack, "stockId");

        wcsStockAutoHistService.createHist(rack);
    }
}