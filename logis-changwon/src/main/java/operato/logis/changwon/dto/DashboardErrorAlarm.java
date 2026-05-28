package operato.logis.changwon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardErrorAlarm {

    private String id;

    private Boolean isChecked;

    private String taskNo;

    private String taskId;

    private String errorMachine;

    private Integer errorCode;

    private String errorName;

    private String orderKind;

    private String startPointCd;

    private String endPointCd;
}
