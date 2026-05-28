package operato.logis.posco.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class McsMonitoringDetailDto {

    private String equipType;

    private String equipCode;

    private String status;

    private Integer positionX;

    private Integer positionY;

    private Integer rotation;

    private Integer battery;

    private String podCode;

    private String podType;

    private String taskId;

    private String taskType;

    private Integer taskPriority;

    private String useYn;

    private String locationCd;

    private String locationNm;

    private String groupCd;

    private String locationType;

    private String stockId;

    private String startPointCd;

    private String endPointCd;

    private Integer processStatus;

    private Date acceptDatetime;

    private Date startDatetime;

    private Date loadingDatetime;

    private Date completeDatetime;

    private String errorCode;

    private String errorMsg;
}