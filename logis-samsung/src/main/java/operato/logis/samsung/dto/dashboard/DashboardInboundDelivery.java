package operato.logis.samsung.dto.dashboard;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.consts.ProcessStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DashboardInboundDelivery {

    private String deliveryNo;
    private String cntrNo;

    private List<String> automationItemCode = new ArrayList<>();
    private List<String> manualItemCode = new ArrayList<>();

    private List<SkuSummary> skuSummary = new ArrayList<>();

    @Getter
    public static class SkuSummary {

        private String sku; // item_code

        private String itemType;

        private String itemDesc;

        // --- 집계 수량들 (Setter 직접 구현) ---
        @Setter(AccessLevel.NONE)
        private Integer totalQty;

        @Setter(AccessLevel.NONE)
        private Integer inboundQty;

        @Setter(AccessLevel.NONE)
        private Integer palletQty;

        // --- 상태 판단용 내부 값들 ---
        @Setter(AccessLevel.NONE)
        private Integer processStatus;   // tb_mw_xyz_order.process_status (0/31/32/33/39)

        @Setter(AccessLevel.NONE)
        private Integer inboundStatus;   // tb_mw_inbound_delivery.inbound_status (1/2/3)

        @Setter(AccessLevel.NONE)
        private Boolean manualFlag;      // 하나라도 manual이면 true

        /** 프론트에서 쓰는 최종 상태 문자열
         *  - WAIT        : 흰색
         *  - IN_PROGRESS : 초록
         *  - COMPLETE    : 회색
         *  - MANUAL      : 노랑
         *  - ERROR       : 빨강
         */
        private String status;

        // ====== 상태 계산 로직 ======
        public void refreshStatus() {
            int total   = nvl(totalQty);
            int inbound = nvl(inboundQty);
            int pallet  = nvl(palletQty);
            Integer procStatus  = processStatus;
            Integer inbStatus  = inboundStatus;
            Boolean manualStatus  = manualFlag != null ? manualFlag : Boolean.FALSE;


            // 1) 수동 전환이 최우선
            if (manualStatus) {
                this.status = "MANUAL";
                return;
            }

            // 2) 완료 상태 (컨테이너 기준 or 수량 기준)
            if ((inbStatus != null && inbStatus == 3) || (total > 0 && pallet >= total)) {
                this.status = "COMPLETE";
                return;
            }

            // 3) 설비 에러
            if (ProcessStatus.ORDER_ERROR.value().equals(procStatus)) {
                this.status = "ERROR";
                return;
            }

            // 4) 진행 중 (설비에서 잡고 있거나, 일부라도 처리됨)
            if ((ProcessStatus.ORDER_START.value().equals(procStatus) || ProcessStatus.ORDER_READY.value().equals(procStatus))
                    || inbound > 0) {
                this.status = "IN_PROGRESS";
                return;
            }

            // 5) 나머지는 대기
            this.status = "WAIT";
        }

        private int nvl(Integer v) {
            return v == null ? 0 : v;
        }
    }
}