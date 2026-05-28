package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.QcTestStatus;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.order.host.HostOrderAuditLogger;
import operato.logis.wcs.service.impl.order.host.HostOrderEvents;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 파렛트 입고 release 처리.
 * BCR 스캔 완료(또는 관리자 우회) 후 host_order 를 READY_FOR_ALLOC 로 전이시킨다.
 */
@Service
@RequiredArgsConstructor
public class PalletReleaseService {

    private static final Logger logger = LoggerFactory.getLogger(PalletReleaseService.class);

    private final PalletBoxRepository palletBoxRepository;
    private final HostOrderRepository hostOrderRepository;
    private final HostOrderStateWriter hostOrderStateWriter;
    private final PalletProgressService palletProgressService;
    private final HostOrderAuditLogger historyLogger;

    /**
     * 파렛트 입고 release.
     * 시험 부적합 차단 → 우회/스캔 검증 → (우회 시) 미스캔 박스 일괄 SCANNED → host 상태 전이.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> release(String palletBarcode, boolean adminBypass) {
        // 파라미터 / 파렛트 / 호스트 주문 검증
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "p");
        List<TbWcsPalletBox> boxes = palletBoxRepository.findByPalletBarcode(palletBarcode);
        requireFound(boxes, "PALLET_NOT_FOUND", palletBarcode);
        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(boxes.get(0).getHostOrderKey());
        if (ValueUtil.isEmpty(host)) {
            throw new ElidomRuntimeException("ORDER_NOT_FOUND", boxes.get(0).getHostOrderKey());
        }

        // 시험 부적합 차단
        if (HostOrderStatus.TEST_FAILED.code() == host.getOrderStatus()
                || QcTestStatus.FAILED.code().equals(host.getTestStatus())) {
            throw new ElidomRuntimeException("TEST_FAILED_BLOCKED", "release blocked");
        }

        // 우회 플래그가 켜져 있어도 실제 관리자 권한이 있어야만 허용
        if (adminBypass && !User.isCurrentUserAdmin()) {
            throw new ElidomRuntimeException("FORBIDDEN", "관리자 권한이 필요합니다.");
        }

        // 우회가 아니고 스캔도 미완료면 차단
        boolean scanDone = palletProgressService.isPalletScanCompleted(palletBarcode);
        if (!adminBypass && !scanDone) {
            Map<String, Object> pr = palletProgressService.progress(palletBarcode);
            throw new ElidomRuntimeException("BOXES_NOT_SCANNED",
                    pr.get("scannedBoxes") + "/" + pr.get("totalBoxes"));
        }

        // 관리자 우회: 미스캔 박스를 일괄 SCANNED (VOID/DEPLETED/DRAFT 제외)
        int bypassedBoxCount = 0;
        if (adminBypass && !scanDone) {
            for (TbWcsPalletBox box : boxes) {
                BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
                if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;
                if (st == BoxStatus.SCANNED) continue;
                if (st == BoxStatus.DRAFT) continue;
                box.setBoxStatus(BoxStatus.SCANNED.code());
                palletBoxRepository.update(box, "boxStatus");
                bypassedBoxCount++;
            }
        }

        // 호스트 주문 상태 전이 (RECEIVED → READY_FOR_ALLOC) + 감사 로그. 전이는 StateWriter 단일 창구 경유
        int prevStatus = host.getOrderStatus();
        if (HostOrderStatus.RECEIVED.code() == prevStatus) {
            hostOrderStateWriter.markReadyForAllocation(host);
            if (adminBypass && !scanDone) {
                Map<String, Object> pr = palletProgressService.progress(palletBarcode);
                String detail = "ADMIN_BYPASS_SCAN_INCOMPLETE bypassedBoxes=%s final=%s/%s"
                        .formatted(bypassedBoxCount, pr.get("scannedBoxes"), pr.get("totalBoxes"));
                historyLogger.log(host, prevStatus, HostOrderEvents.BCR_RELEASED, null, detail);
            } else {
                historyLogger.log(host, prevStatus, HostOrderEvents.BCR_RELEASED, null, "BCR release");
            }
        }

        logger.info("[ Pallet ][ Bcr ] release - palletBarcode={}, hostOrderKey={}, adminBypass={}, bypassedBoxes={}",
                palletBarcode, host.getHostOrderKey(), adminBypass, bypassedBoxCount);

        // 응답
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("palletBarcode", palletBarcode);
        result.put("hostOrderKey", host.getHostOrderKey());
        result.put("orderStatus", host.getOrderStatus());
        result.put("released", true);
        if (adminBypass) {
            result.put("adminBypass", true);
            result.put("bypassedBoxCount", bypassedBoxCount);
        }
        return result;
    }
}
