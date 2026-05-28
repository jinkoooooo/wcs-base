package operato.logis.lms.dto.dashboard;

import lombok.Getter;
import lombok.Setter;
import operato.logis.lms.entity.dashboard.StatusBoardStock;

import java.util.List;

@Getter
@Setter
public class StatusBoardWcsInfo {

    private String taskId;

    private String commandType;

    private String startPointCd;

    private String endPointCd;

    private String stockId;

    private List<StatusBoardStock> stockInfoList;
}