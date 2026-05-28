package operato.logis.wcs.service.impl.qctest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.generator.QcTestRequestNoGenerator;
import operato.logis.wcs.common.util.lang.ParseUtils;
import operato.logis.wcs.consts.QcTestRequestStatus;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.qctest.QcRequestBatchSave;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.QcTestRequestRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * (입고일자, SKU, LOT) 단위 시험 의뢰 운영 서비스. IF02 발신 이력 스냅샷 관리.
 *
 * manufacturer / item_owner / mfr_unit 은 의뢰 저장 시점에 item_code 로 마스터를 조회해 복사한다.
 * req_dept / submitter_order 는 화면 입력값.
 */
@Service
@RequiredArgsConstructor
public class QcRequestService {

    private static final Logger logger = LoggerFactory.getLogger(QcRequestService.class);

    private final QcTestRequestRepository repository;
    private final QcTestRequestNoGenerator requestNoGenerator;
    private final ObjectMapper objectMapper;
    private final InventoryItemMasterRepository itemMasterRepository;

    @Transactional(readOnly = true)
    public TbWcsQcTestRequest lookup(LocalDate inboundDate, String itemCode, String lotNo) {
        validateKey(inboundDate, itemCode);
        return repository.findByDateAndItemAndLot(inboundDate, itemCode, lotNo);
    }

    @Transactional(readOnly = true)
    public TbWcsQcTestRequest findById(String id) {
        return repository.findById(id);
    }

    /**
     * 신규 시험 의뢰 발행. test_request_no 자동 발번.
     * manufacturer/item_owner/mfr_unit 은 item_code 로 마스터 조회해 복사.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsQcTestRequest createWithPdfId(LocalDate inboundDate, String itemCode, String lotNo,
                                              String pdfFileId,
                                              String testWfType, String testReqDesc,
                                              Date manufacturedDate, Date expiryDate,
                                              Integer manufacturedQty, Integer incomingQty,
                                              String reqDept, String submitterOrder) {
        validateKey(inboundDate, itemCode);

        String normalizedLot = ValueUtil.isEmpty(lotNo) ? "" : lotNo;

        TbWcsQcTestRequest existing = repository.findByDateAndItemAndLot(inboundDate, itemCode, normalizedLot);
        if (ValueUtil.isNotEmpty(existing)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "이미 존재하는 의뢰입니다 (date=%s, itemCode=%s, lot=%s).".formatted(inboundDate, itemCode, normalizedLot));
        }

        TbWcsQcTestRequest e = new TbWcsQcTestRequest();
        e.setInboundDate(LocalDateUtils.toDate(inboundDate));
        e.setItemCode(itemCode);
        e.setLotNo(normalizedLot);
        e.setTestRequestNo(requestNoGenerator.generate(inboundDate));
        e.setFetched(Boolean.FALSE);
        e.setReportPdfId(pdfFileId);

        // 화면 입력값
        e.setTestWfType(testWfType);
        e.setManufacturedDate(manufacturedDate);
        e.setExpiryDate(expiryDate);
        e.setManufacturedQty(manufacturedQty);
        e.setIncomingQty(incomingQty);
        e.setReqDept(reqDept);
        e.setSubmitterOrder(submitterOrder);

        // 마스터 스냅샷 복사 (item_code 단독)
        ExtTbInventoryItemMaster master = requireItemMaster(itemCode);
        applyMasterSnapshot(e, master);

        // 의뢰내용 — 화면 미입력이면 마스터 item_category 유도
        e.setTestReqDesc(ValueUtil.isNotEmpty(testReqDesc) ? testReqDesc : resolveTestReqDesc(master));

        e.setStatus(resolveStatus(e).code());

        repository.insert(e);

        logger.info("[ Qctest ][ Request ] created - id={}, date={}, itemCode={}, lot={}, owner={}, reqNo={}, fileId={}, mfr={}, mfrUnit={}, status={}, wfType={}, reqDesc={}, reqDept={}, submitter={}",
                e.getId(), inboundDate, itemCode, normalizedLot, e.getItemOwner(), e.getTestRequestNo(),
                pdfFileId, e.getManufacturer(), e.getMfrUnit(), e.getStatus(), testWfType,
                e.getTestReqDesc(), reqDept, submitterOrder);
        return e;
    }

    /**
     * 그리드 일괄 저장 — (입고일자, SKU, lot) 키로 upsert. 단일 트랜잭션.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSave(List<QcRequestBatchSave.QcRequestEntry> entries) {
        if (ValueUtil.isEmpty(entries)) {
            return Map.of("success", true, "createdCount", 0, "updatedCount", 0);
        }

        int created = 0;
        int updated = 0;

        for (QcRequestBatchSave.QcRequestEntry en : entries) {
            LocalDate inboundDate = ParseUtils.parseDate(en.getInboundDate());
            validateKey(inboundDate, en.getItemCode());
            String normalizedLot = ValueUtil.isEmpty(en.getLotNo()) ? "" : en.getLotNo();

            TbWcsQcTestRequest existing =
                    repository.findByDateAndItemAndLot(inboundDate, en.getItemCode(), normalizedLot);

            if (ValueUtil.isEmpty(existing)) {
                createWithPdfId(
                        inboundDate, en.getItemCode(), normalizedLot, en.getFileId(),
                        en.getTestWfType(), en.getTestReqDesc(),
                        ParseUtils.parseDateToDate(en.getManufacturedDate()),
                        ParseUtils.parseDateToDate(en.getExpiryDate()),
                        en.getManufacturedQty(), en.getIncomingQty(),
                        en.getReqDept(), en.getSubmitterOrder());
                created++;
            } else {
                applyUpdate(existing, en);
                updated++;
            }
        }

        logger.info("[ Qctest ][ Request ] batchSave - total={}, created={}, updated={}",
                entries.size(), created, updated);
        return Map.of("success", true, "createdCount", created, "updatedCount", updated);
    }

    /**
     * 기존 의뢰 수정. 화면 입력은 null-safe 병합, 마스터 스냅샷 3종은 저장 시점 최신값으로 항상 재복사.
     * COMPLETED 건은 수정 불가.
     */
    private void applyUpdate(TbWcsQcTestRequest e, QcRequestBatchSave.QcRequestEntry en) {
        if (QcTestRequestStatus.COMPLETED.code().equals(e.getStatus())) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "이미 결과가 수신된 의뢰는 수정할 수 없습니다. reqNo=" + e.getTestRequestNo());
        }

        Date manufacturedDate = ParseUtils.parseDateToDate(en.getManufacturedDate());
        Date expiryDate = ParseUtils.parseDateToDate(en.getExpiryDate());

        List<String> dirty = new ArrayList<>();

        if (ValueUtil.isNotEmpty(en.getTestWfType())) {
            e.setTestWfType(en.getTestWfType());
            dirty.add("testWfType");
        }
        if (ValueUtil.isNotEmpty(en.getTestReqDesc())) {
            e.setTestReqDesc(en.getTestReqDesc());
            dirty.add("testReqDesc");
        }
        if (ValueUtil.isNotEmpty(manufacturedDate)) {
            e.setManufacturedDate(manufacturedDate);
            dirty.add("manufacturedDate");
        }
        if (ValueUtil.isNotEmpty(expiryDate)) {
            e.setExpiryDate(expiryDate);
            dirty.add("expiryDate");
        }
        if (en.getManufacturedQty() != null) {
            e.setManufacturedQty(en.getManufacturedQty());
            dirty.add("manufacturedQty");
        }
        if (en.getIncomingQty() != null) {
            e.setIncomingQty(en.getIncomingQty());
            dirty.add("incomingQty");
        }
        if (ValueUtil.isNotEmpty(en.getReqDept())) {
            e.setReqDept(en.getReqDept());
            dirty.add("reqDept");
        }
        if (ValueUtil.isNotEmpty(en.getSubmitterOrder())) {
            e.setSubmitterOrder(en.getSubmitterOrder());
            dirty.add("submitterOrder");
        }
        if (ValueUtil.isNotEmpty(en.getFileId())) {
            e.setReportPdfId(en.getFileId());
            dirty.add("reportPdfId");
        }

        // 마스터 스냅샷 재복사 — 저장 시점 최신값 (item_code 기준)
        ExtTbInventoryItemMaster master = itemMasterRepository.findByCode(e.getItemCode());
        if (master != null) {
            e.setManufacturer(master.getManufacturer());
            e.setItemOwner(master.getItemOwner());
            e.setMfrUnit(master.getItemUnit());
            dirty.add("manufacturer");
            dirty.add("itemOwner");
            dirty.add("mfrUnit");
            if (ValueUtil.isEmpty(e.getTestReqDesc())) {
                String derived = resolveTestReqDesc(master);
                if (ValueUtil.isNotEmpty(derived)) {
                    e.setTestReqDesc(derived);
                    dirty.add("testReqDesc");
                }
            }
        }

        String newStatus = resolveStatus(e).code();
        if (!newStatus.equals(e.getStatus())) {
            e.setStatus(newStatus);
            dirty.add("status");
        }

        if (dirty.isEmpty()) {
            logger.info("[ Qctest ][ Request ] update skip (no changes) - id={}, reqNo={}",
                    e.getId(), e.getTestRequestNo());
            return;
        }

        repository.update(e, dirty.toArray(new String[0]));
        logger.info("[ Qctest ][ Request ] fields updated - id={}, reqNo={}, status={}, changed={}",
                e.getId(), e.getTestRequestNo(), e.getStatus(), dirty);
    }

    /** 마스터 스냅샷 3종 복사 — manufacturer / item_owner / mfr_unit(=item_unit). */
    private void applyMasterSnapshot(TbWcsQcTestRequest e, ExtTbInventoryItemMaster master) {
        if (master == null) return;
        e.setManufacturer(master.getManufacturer());
        e.setItemOwner(master.getItemOwner());
        e.setMfrUnit(master.getItemUnit());
    }

    /**
     * 필수값 충족 시 PENDING, 아니면 DRAFT. COMPLETED 는 유지.
     */
    private QcTestRequestStatus resolveStatus(TbWcsQcTestRequest e) {
        if (QcTestRequestStatus.COMPLETED.code().equals(e.getStatus())) {
            return QcTestRequestStatus.COMPLETED;
        }
        boolean ready =
                ValueUtil.isNotEmpty(e.getTestWfType())
                        && ValueUtil.isNotEmpty(e.getTestReqDesc())
                        && ValueUtil.isNotEmpty(e.getManufacturer())
                        && ValueUtil.isNotEmpty(e.getLotNo())
                        && ValueUtil.isNotEmpty(e.getManufacturedDate())
                        && ValueUtil.isNotEmpty(e.getMfrUnit())
                        && ValueUtil.isNotEmpty(e.getExpiryDate())
                        && ValueUtil.isNotEmpty(e.getIncomingQty())
                        && ValueUtil.isNotEmpty(e.getReqDept())
                        && ValueUtil.isNotEmpty(e.getSubmitterOrder())
                        && ValueUtil.isNotEmpty(e.getItemOwner());
        // manufacturedQty, reportPdfId 는 선택 — 조건 제외
        return ready ? QcTestRequestStatus.PENDING : QcTestRequestStatus.DRAFT;
    }

    /**
     * item_code 로 마스터 1회 조회. 누락 시 null, 없으면 예외.
     */
    private ExtTbInventoryItemMaster requireItemMaster(String itemCode) {
        if (ValueUtil.isEmpty(itemCode)) {
            logger.warn("[ Qctest ][ Request ] item master lookup skip - itemCode 누락");
            return null;
        }
        ExtTbInventoryItemMaster m = itemMasterRepository.findByCode(itemCode);
        if (ValueUtil.isEmpty(m)) {
            logger.warn("[ Qctest ][ Request ] item master not found - itemCode={}", itemCode);
            throw new ElidomRuntimeException(WcsError.NO_AVAILABLE_ITEM_MST.code(),
                    "존재하지 않는 자재 코드입니다. (itemCode=%s).".formatted(itemCode));
        }
        return m;
    }

    /**
     * 의뢰내용 자동 유도 — 마스터 item_category. 없으면 null.
     */
    private String resolveTestReqDesc(ExtTbInventoryItemMaster master) {
        if (master == null || ValueUtil.isEmpty(master.getItemCategory())) {
            logger.debug("[ Qctest ][ Request ] test_req_desc auto-fill skip - no item_category");
            return null;
        }
        return master.getItemCategory();
    }

    @Transactional(rollbackFor = Exception.class)
    public TbWcsQcTestRequest replacePdf(String id, String newPdfFileId) {
        TbWcsQcTestRequest e = requireById(id);
        if (ValueUtil.isEmpty(newPdfFileId)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "newPdfFileId 가 비어있습니다.");
        }
        e.setReportPdfId(newPdfFileId);
        repository.update(e, "reportPdfId");
        logger.info("[ Qctest ][ Request ] pdf replaced - id={}, reqNo={}, newFileId={}",
                id, e.getTestRequestNo(), newPdfFileId);
        return e;
    }

    @Transactional(readOnly = true)
    public List<TbWcsQcTestRequest> findToday() {
        List<TbWcsQcTestRequest> byInboundDate = repository.findByInboundDate(LocalDate.now());
        try {
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(byInboundDate);
            logger.info("[ Qctest ][ Request ] findToday - count={}, data=\n{}",
                    byInboundDate != null ? byInboundDate.size() : 0, jsonString);
        } catch (Exception e) {
            logger.error("[ Qctest ][ Request ] log serialization failed", e);
        }
        return byInboundDate;
    }

    @Transactional(readOnly = true)
    public List<TbWcsQcTestRequest> findByDate(LocalDate date) {
        return repository.findByInboundDate(ValueUtil.isEmpty(date) ? LocalDate.now() : date);
    }

    @Transactional(readOnly = true)
    public List<TbWcsQcTestRequest> findUnfetched() {
        return repository.findUnfetched();
    }

    @Transactional(readOnly = true)
    public List<TbWcsQcTestRequest> findByStatus(QcTestRequestStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional(rollbackFor = Exception.class)
    public TbWcsQcTestRequest markFetched(String id) {
        TbWcsQcTestRequest e = requireById(id);
        if (Boolean.TRUE.equals(e.getFetched())) return e;
        e.setFetched(Boolean.TRUE);
        repository.update(e, "fetched");
        logger.info("[ Qctest ][ Request ] fetched - id={}, reqNo={}", id, e.getTestRequestNo());
        return e;
    }

    @Transactional(rollbackFor = Exception.class)
    public TbWcsQcTestRequest complete(String id, String testNo) {
        TbWcsQcTestRequest e = requireById(id);
        if (ValueUtil.isEmpty(testNo)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "test_no 가 비어있습니다. id=" + id);
        }
        e.setTestNo(testNo);
        e.setStatus(QcTestRequestStatus.COMPLETED.code());
        e.setCompletedAt(new Date());
        repository.update(e, "testNo", "status", "completedAt");
        logger.info("[ Qctest ][ Request ] completed (LIMS callback) - id={}, reqNo={}, testNo={}",
                id, e.getTestRequestNo(), testNo);
        return e;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteAll(List<String> ids) {
        if (ValueUtil.isEmpty(ids)) return 0;
        int n = 0;
        for (String id : ids) {
            TbWcsQcTestRequest e = repository.findById(id);
            if (ValueUtil.isEmpty(e)) continue;
            repository.delete(e);
            n++;
        }
        logger.info("[ Qctest ][ Request ] deleteAll - requested={}, actual={}", ids.size(), n);
        return n;
    }

    private TbWcsQcTestRequest requireById(String id) {
        if (ValueUtil.isEmpty(id)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(), "id 는 필수입니다.");
        }
        TbWcsQcTestRequest e = repository.findById(id);
        if (ValueUtil.isEmpty(e)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "qc_test_request 없음. id=" + id);
        }
        return e;
    }

    private void validateKey(LocalDate inboundDate, String itemCode) {
        if (ValueUtil.isEmpty(inboundDate)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "inboundDate 는 필수입니다.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "itemCode 는 필수입니다.");
        }
    }
}