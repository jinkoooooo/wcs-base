package operato.logis.changwon.dto.lms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


@Data
@Builder
public class EquipmentRequestDto {

    private String lcId;
    private String equipId;
    private String lineId;
    private String orderId;

    private String currentStatus;
    private String preStatus;

    private String errCd;
    private String errMsg;

    private BigDecimal sensorValue;
    private String sensorUnit;

    private Integer operatingCnt;
    private Integer errCnt;

    private Date statusUpdatedAt;
    private Date dataUpdatedAt;
    private String sourceSystem;
}
