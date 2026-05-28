package operato.logis.samsung.dto.dashboard;

import lombok.Data;

@Data
public class DashboardUPH {
    // 시간 구간 (예: "09:00", "09:30")
    private String timeSlot;

    // 처리 수량 (Box 단위)
    private int qty;

    public DashboardUPH(String timeSlot, int qty) {
        this.timeSlot = timeSlot;
        this.qty = qty;
    }
}