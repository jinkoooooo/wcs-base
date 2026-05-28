package operato.logis.samsung.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardInboundStatus {

    private int inboundPlanCntrQty; // 입고 예정 컨테이너 수량

    private int completedCntrQty; // 입고 완료 컨테이너 수량

    private int inboundPlanBoxQty; // 입고 예정 총 박스 수량

    private int autoCompletedQty; // 설비 입고 파렛타이징 완료 박스 수량

    private int manualCompletedQty; // 메뉴얼 입고 파렛타이징 완료 박스 수량

    private int ngBoxQty; // 입고 불량 박스 수량
}