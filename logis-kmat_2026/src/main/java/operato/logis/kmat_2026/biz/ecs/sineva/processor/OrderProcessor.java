package operato.logis.kmat_2026.biz.ecs.sineva.processor;

import operato.logis.kmat_2026.entity.TbWcsOrder;

/**
 * ============================================================================
 * 공통 Processor 인터페이스
 * ============================================================================
 *
 * [역할]
 * - execute : 신규 흐름 시작
 * - callback : 작업 callback 후속 처리
 *
 * [원칙]
 * - processor는 흐름 제어만 담당한다.
 * - 실제 DB 저장/상태 변경/외부 인터페이스/락 제어는 service에 위임한다.
 */
public interface OrderProcessor<R> {

    R execute(String param);

    R callback(TbWcsOrder order) throws InterruptedException;

    String getProcessorType();
}