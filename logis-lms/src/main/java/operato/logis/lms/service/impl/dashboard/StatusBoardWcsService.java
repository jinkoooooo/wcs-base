package operato.logis.lms.service.impl.dashboard;

import operato.logis.lms.entity.dashboard.StatusBoardStock;
import operato.logis.lms.dto.dashboard.StatusBoardWcsInfo;
import operato.logis.lms.entity.dashboard.StatusBoardTask;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class StatusBoardWcsService extends AbstractQueryService {

    public StatusBoardWcsInfo getWcsInfo(String lcId, String taskId, String stockId) {
        StatusBoardWcsInfo info = new StatusBoardWcsInfo();

        Map<String, Object> param = ValueUtil.newMap("lcId,taskId,stockId", lcId, taskId, stockId);
        String taskSql = "select * from status_board_task where lc_id = :lcId and task_id = :taskId order by created_at desc limit 1";
        StatusBoardTask task = this.queryManager.selectBySql(taskSql, param, StatusBoardTask.class);
        info.setTaskId(taskId);
        if (ValueUtil.isNotEmpty(task)) {
            info.setCommandType(task.getCommandType());
            info.setStartPointCd(task.getStartPointCd());
            info.setEndPointCd(task.getEndPointCd());
        }

        String stockSql = "select * from status_board_stock where lc_id = :lcId and stock_id = :stockId order by created_at desc";
        List<StatusBoardStock> stockList = this.queryManager.selectListBySql(stockSql, param, StatusBoardStock.class, 0, 0);
        info.setStockId(stockId);
        if (ValueUtil.isNotEmpty(stockList)) {
            info.setStockInfoList(stockList);
        }

        return info;
    }
}
