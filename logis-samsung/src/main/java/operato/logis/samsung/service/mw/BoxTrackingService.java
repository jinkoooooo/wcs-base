package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.dto.dashboard.DashboardInboundTracking;
import operato.logis.samsung.entity.mw.TbMwBox;
import operato.logis.samsung.entity.mw.TbMwBoxTrack;
import operato.logis.samsung.event.BoxTrackingEvent;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxTrackingService extends AbstractQueryService {

    /* =========================================================
     * 조회 헬퍼
     * ========================================================= */
    public TbMwBox findBoxByParcelId(String parcelId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("parcel_id", parcelId);

        return this.queryManager.select(TbMwBox.class, condition);
    }

    // 해당 plc_seq_no 트레킹 최신 로우 추출
    public TbMwBoxTrack findBoxTrackByPlcSeqNo(String plcSeqNo) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("plc_seq_no", plcSeqNo);
        condition.addOrder("tracking_at", false); // desc
        condition.setPageSize(1);

        return this.queryManager.select(TbMwBoxTrack.class, condition);
    }

    // 해당 박스 트레킹 이력 조회
    public TbMwBoxTrack findBoxTrackByStatus(String boxId, TrackingStatus status) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("box_id", boxId);
        condition.addFilter("tracking_status", status.getValue());
        condition.addOrder("tracking_at", false); // desc
        condition.setPageSize(1);

        return this.queryManager.select(TbMwBoxTrack.class, condition);
    }

    // 해당 plc_seq_no 트레킹 최신 로우 추출
    public TbMwBox findBoxByPlcSeqNo(String plcSeqNo) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("plc_seq_no", plcSeqNo);
        condition.addOrder("received_at", false); // desc
        condition.setPageSize(1);

        return this.queryManager.select(TbMwBox.class, condition);
    }

    public TbMwBox findBoxByKey(String boxId, String parcelId) {

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("box_id", boxId);
        condition.addFilter("parcel_id", parcelId);

        return this.queryManager.select(TbMwBox.class, condition);
    }
    public TbMwBox findBoxByBoxId(String boxId) {

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("box_id", boxId);
        condition.addOrder("received_at", false); // desc
        condition.setPageSize(1);

        return this.queryManager.select(TbMwBox.class, condition);
    }

    public List<TbMwBox> findBoxesByCondition(String date,
                                              String dateMode,
                                              String startDate,
                                              String endDate,
                                              String barcode,
                                              String serial,
                                              String rejectType) {

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());

        // ✅ 1) 날짜 필수 조건
        // 컬럼명이 received_at(타임스탬프) 라고 가정. 필요하면 inbound_date 등으로 바꿔줘.
        // 하루치 조회 예시: date = '2025-11-26'

        String fromDateTime = "";
        String toDateTime   = "";

        if ("range".equalsIgnoreCase(dateMode)){
            fromDateTime = startDate + " 00:00:00";
            toDateTime   = endDate + " 23:59:59";
        }else{
            fromDateTime = date + " 00:00:00";
            toDateTime   = date + " 23:59:59";

        }

        condition.addFilter("tracking_at", ">=", fromDateTime);
        condition.addFilter("tracking_at", "<=", toDateTime);

        // ✅ 2) 옵션 조건들 (null / '' 이면 필터 추가하지 않음)
        if (!ValueUtil.isEmpty(barcode)) {
            condition.addFilter("item_code", barcode);
        }
        if (!ValueUtil.isEmpty(serial)) {
            condition.addFilter("box_id", serial);
        }
        if (!ValueUtil.isEmpty(rejectType)) {
            condition.addFilter("reject_type", rejectType);
        }
        // 정렬: 최신 박스가 위로 오도록
        condition.addOrder("tracking_at", false); // desc

        // 리스트 조회
        return this.queryManager.selectList(TbMwBox.class, condition);
    }

    /* =========================================================
     * BOX 생성 / BOX 갱신 / TRACK 인서트 (공용)
     * ========================================================= */
    public TbMwBox insertTbMwBox(BoxTrackingEvent e, String parcelId,
                                  String lineId, String equipId, Date now) {
        TbMwBox box = new TbMwBox();
        box.setParcelId(parcelId);
        box.setBoxId(e.getBarcode());
        box.setItemCode(e.getItemCode());
        box.setReceivedAt(now);
        box.setFirstLineId(lineId);
        box.setFirstEquipId(equipId);
        box.setRejectType("정상");

        box.setPlcSeqNo(e.getPlcSeqNo());

        box.setTrackingStatus(TrackingStatus.FIRST_INPUT.getValue());
        box.setTrackingDesc(TrackingStatus.FIRST_INPUT.getDescription());
        box.setTrackingAt(now);

        // 비전 결과
        if (e.getCognexVisionResult() != null)  box.setCognexResult(e.getCognexVisionResult());
        // 규격/방향
        if (e.getBoxLength() != null)  box.setBoxLength(e.getBoxLength());
        if (e.getBoxWidth()  != null)  box.setBoxWidth(e.getBoxWidth());
        if (e.getBoxHeight() != null)  box.setBoxHeight(e.getBoxHeight());
        if (e.getBoxAngle()  != null)  box.setBoxAngle(e.getBoxAngle());

        this.queryManager.insert(box);
        return box;
    }

    public void updateTbMwBoxByEvent(TbMwBox box, BoxTrackingEvent e,
                               String lineId, String equipId, Date now) {


        box.setPlcSeqNo(e.getPlcSeqNo());

        if (ValueUtil.isEmpty(box.getReceivedAt())) box.setReceivedAt(now);
        if (ValueUtil.isEmpty(box.getFirstLineId()))  box.setFirstLineId(lineId);
        if (ValueUtil.isEmpty(box.getFirstEquipId())) box.setFirstEquipId(equipId);
        if (ValueUtil.isNotEmpty(e.getItemCode())) {
            box.setItemCode(e.getItemCode());
        }

        // 삼성 비전 결과 저장
        if (ValueUtil.isNotEmpty(e.getSamsungVisionResult())) {
            box.setSdsAiResult(e.getSamsungVisionResult());
            box.setSdsAiResultAt(new Date());
        }

        // 메뉴얼 비전 결과 저장
        if (ValueUtil.isNotEmpty(e.getManualVisionResult())) {
            box.setManualResult(e.getManualVisionResult());
            box.setManualResultAt(new Date());
        }

        if (e.getFileNameTop() != null)         box.setFileNameTop(e.getFileNameTop());
        if (e.getFileNameFront() != null)       box.setFileNameFront(e.getFileNameFront());
        if (e.getFileNameBack() != null)        box.setFileNameBack(e.getFileNameBack());
        if (e.getFileNameLeft() != null)        box.setFileNameLeft(e.getFileNameLeft());
        if (e.getFileNameRight() != null)       box.setFileNameRight(e.getFileNameRight());
        if (e.getFileNameBottomLeft() != null)  box.setFileNameBottomLeft(e.getFileNameBottomLeft());
        if (e.getFileNameBottomRight() != null) box.setFileNameBottomRight(e.getFileNameBottomRight());

        // 박스 체적값 저장
        if (e.getBoxAngle()  != null)   box.setBoxAngle(e.getBoxAngle());
        if (e.getBoxLength()  != null)  box.setBoxLength(e.getBoxLength());
        if (e.getBoxWidth()  != null)   box.setBoxWidth(e.getBoxWidth());
        if (e.getBoxHeight()  != null)  box.setBoxHeight(e.getBoxHeight());

        box.setUpdatedAt(now);
        this.queryManager.update(box);
    }
    public void updateTbMwBox(TbMwBox box) {

        box.setUpdatedAt(new Date());
        this.queryManager.update(box);
    }

    // 현재 박스 트레킹 상태 업데이트 공용 메서드
    public void updateTbMwBoxStatus(String parcelId, TrackingStatus ts) {

        String updateSql = "UPDATE tb_mw_box " +
                "SET tracking_status = :trackingStatus "  +
                ", tracking_desc = :trackingDesc "  +
                ", tracking_at = :trackingAt "  +
                "WHERE parcel_id = :parcelId ";

        this.queryManager.executeBySql(
                updateSql,
                ValueUtil.newMap("trackingStatus,trackingDesc,trackingAt,parcelId",
                        ts.getValue(), ts.getDescription(), new Date(), parcelId)
        );
    }

    public TbMwBoxTrack insertTrackSnapshot(TbMwBox box, String plcSeqNo, String lineId, String equipId,
                                             Date when, TrackingType tType, TrackingStatus tStatus) {
        TbMwBoxTrack tr = new TbMwBoxTrack();
        tr.setParcelId(box.getParcelId());
        tr.setBoxId(box.getBoxId());
        tr.setPlcSeqNo(plcSeqNo);
        tr.setLineId(lineId);
        tr.setEquipId(equipId);
        tr.setTrackingType(tType.name());
        tr.setTrackingStatus(tStatus.getValue());
        tr.setTrackingDesc(tStatus.getDescription());
        tr.setTrackingAt(when);

        this.queryManager.insert(tr);

        return tr;
    }

    // 박스 Reject 상태 업데이트
    public void updateRejectStatus(TbMwBox box, String rejectDesc, String rejectType) {
        box.setRejectDesc(rejectDesc);
        box.setRejectType(rejectType);
        this.queryManager.update(box, "rejectDesc", "rejectType");
    }

    /**
     * 전체 유닛 상태 조회
     *
     * @return 스큐별 박스입고/파렛타이징 수량 리스트
     */
    public List<DashboardInboundTracking> getAllBoxTracking(Date targetDate) {
        String sql = "with a as(\n" +
                "    SELECT t.box_id,\n" +
                "           t.tracking_status,\n" +
                "           t.plc_seq_no,\n" +
                "           t.parcel_id,\n" +
                "           max(b.item_code) item_barcode\n" +
                "    FROM tb_mw_box_track t\n" +
                "             left join tb_mw_box b\n" +
                "                       on t.box_id = b.box_id\n" +
                "    WHERE t.tracking_at::date = :targetDate\n" +
                "      and t.tracking_status in (120, 531)\n" +
                "    group by t.box_id,t.tracking_status, t.plc_seq_no, t.parcel_id\n" +
                "    having count(*) = 1\n" +
                ")\n" +
                "select item_barcode,\n" +
                "       tracking_status,\n" +
                "       count(*) box_qty\n" +
                "from a\n" +
                "group by item_barcode, tracking_status";

        Map<String, Object> param = ValueUtil.newMap(
                "targetDate",
                new java.sql.Date(targetDate.getTime())   // date 컬럼에 맞게 변환
        );
        return this.queryManager.selectListBySql(sql, param, DashboardInboundTracking.class, 0, 0);
    }
}
