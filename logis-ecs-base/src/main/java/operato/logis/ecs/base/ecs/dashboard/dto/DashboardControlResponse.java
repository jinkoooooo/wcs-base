package operato.logis.ecs.base.ecs.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드 팝업 제어 명령 실행 결과 DTO
 *
 * [설계 의도]
 * 제어 API의 모든 응답을 단일 타입으로 통일한다.
 * UI는 success 플래그만 보고 토스트 표시를 결정하고,
 * data 필드로 갱신된 상태를 받아 화면을 즉시 반영한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardControlResponse {

    /** 처리 성공 여부 */
    private boolean success;

    /** 운영자에게 보여줄 결과 메시지 */
    private String message;

    /**
     * 처리 후 변경된 상태 데이터 (선택적)
     * 예: 토글 후 새로운 useYn 값, 생성된 재고 ID 등
     */
    private Object data;

    // ====== 정적 팩토리 메서드 (표준 응답 생성) ======

    public static DashboardControlResponse ok(String message) {
        return DashboardControlResponse.builder().success(true).message(message).build();
    }

    public static DashboardControlResponse ok(String message, Object data) {
        return DashboardControlResponse.builder().success(true).message(message).data(data).build();
    }

    public static DashboardControlResponse fail(String message) {
        return DashboardControlResponse.builder().success(false).message(message).build();
    }
}
