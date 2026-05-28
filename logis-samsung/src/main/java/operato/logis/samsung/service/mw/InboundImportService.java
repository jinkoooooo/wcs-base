package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwInboundJob;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboundImportService extends AbstractQueryService {


    private final TbMwItemMasterService tbMwItemMasterService;

    @Transactional
    public Map<String, Object> insertImportData(List<TbMwInboundDelivery> items) {

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("requestItemCount", items == null ? 0 : items.size());
        result.put("missingMaterialCount", 0);
        result.put("missingMaterials", Collections.emptyList());

        if (items == null || items.isEmpty()) {
            result.put("insertedJobCount", 0);
            result.put("insertedDeliveryCount", 0);
            result.put("skippedGroupCount", 0);
            result.put("skippedGroups", Collections.emptyList());
            result.put("message", "업로드 데이터가 없습니다.");
            return result;
        }

        // 0) 필수값 validation (모든 로우에서 필수)
        validateRequired(items);

        // 1) (bl_no, cntr_no) 그룹핑
        Map<String, List<TbMwInboundDelivery>> grouped = new LinkedHashMap<String, List<TbMwInboundDelivery>>();
        for (TbMwInboundDelivery it : items) {
            String bl = safeTrim(it.getBlNo());
            String cn = safeTrim(it.getCntrNo());
            String key = makeKey(bl, cn);

            if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<TbMwInboundDelivery>());
            grouped.get(key).add(it);
        }

        // Item Master 존재 확인 + inner_item_code 세팅
        Set<String> missingMaterials = applyInnerItemCodeOrCollectMissing(grouped);

        // 상품마스터 없는 item_code가 있으면 "저장하지 않고" 프론트로 리턴
        if (!missingMaterials.isEmpty()) {
            result.put("insertedJobCount", 0);
            result.put("insertedDeliveryCount", 0);
            result.put("skippedGroupCount", 0);
            result.put("skippedGroups", Collections.emptyList());

            result.put("missingMaterialCount", missingMaterials.size());
            result.put("missingMaterials", new ArrayList<String>(missingMaterials));
            result.put("message", "상품마스터에 존재하지 않는 item_code(material)가 포함되어 업로드가 중단되었습니다.");
            return result;
        }

        // 2) 기존 tb_mw_inbound_job 중 동일 (bl_no, cntr_no) 존재 여부 확인
        Set<String> existingKeys = findExistingJobKeys(grouped);

        // 3) 저장 대상/스킵 대상 분리 + Job 생성 + Delivery 기본값 세팅
        List<TbMwInboundJob> jobsToInsert = new ArrayList<TbMwInboundJob>();
        List<TbMwInboundDelivery> deliveriesToInsert = new ArrayList<TbMwInboundDelivery>();
        List<Map<String, String>> skippedGroups = new ArrayList<Map<String, String>>();

        Date now = new Date();
        String actor = resolveActorId();

        for (Map.Entry<String, List<TbMwInboundDelivery>> e : grouped.entrySet()) {
            String key = e.getKey();
            List<TbMwInboundDelivery> rows = e.getValue();

            String blNo = safeTrim(rows.get(0).getBlNo());
            String cntrNo = safeTrim(rows.get(0).getCntrNo());

            // 기존 데이터 있으면 그룹 전체 제외
            if (existingKeys.contains(key)) {
                Map<String, String> g = new HashMap<String, String>();
                g.put("bl_no", blNo);
                g.put("cntr_no", cntrNo);
                skippedGroups.add(g);
                continue;
            }

            // Job 1건 생성(그룹당)
            TbMwInboundJob job = buildJobFromGroup(rows, actor, now);
            jobsToInsert.add(job);

            // 상품 입고수량 많은 순 inbound_seq 초기 부여를 위한 솔팅
            rows.sort((a, b) -> Integer.compare(
                    b.getItemQty() == null ? 0 : b.getItemQty(),
                    a.getItemQty() == null ? 0 : a.getItemQty()
            ));
            int seq = 1;
            for (TbMwInboundDelivery d : rows) {
                // Delivery 기본값/필수값 채움
                applyDefaultsToDelivery(d, seq++, actor, now);
                deliveriesToInsert.add(d);
            }
        }

        // 4) 저장 (멀티 VALUES / chunk)
        int insertedJobCount = bulkInsertJobs(jobsToInsert, 300);
        int insertedDeliveryCount = bulkInsertDeliveries(deliveriesToInsert, 500);

        result.put("insertedJobCount", insertedJobCount);
        result.put("insertedDeliveryCount", insertedDeliveryCount);
        result.put("skippedGroupCount", skippedGroups.size());
        result.put("skippedGroups", skippedGroups);
        result.put("message", "업로드 처리 완료");

        return result;
    }

    /* ===================== validation ===================== */

    private void validateRequired(List<TbMwInboundDelivery> items) {
        List<String> errors = new ArrayList<String>();

        for (int i = 0; i < items.size(); i++) {
            TbMwInboundDelivery r = items.get(i);

            String bl = safeTrim(r.getBlNo());
            String cn = safeTrim(r.getCntrNo());
            String itemCode = safeTrim(r.getItemCode());

            // item_qty / inbound_date / 필수들
            if (ValueUtil.isEmpty(bl)) errors.add("[" + (i + 2) + "행] bl_no 필수");
            if (ValueUtil.isEmpty(cn)) errors.add("[" + (i + 2) + "행] cntr_no 필수");
            if (ValueUtil.isEmpty(itemCode)) errors.add("[" + (i + 2) + "행] item_code 필수");
            if (r.getItemQty() == null) errors.add("[" + (i + 2) + "행] item_qty 필수");
            if (r.getInboundDate() == null) errors.add("[" + (i + 2) + "행] inbound_date 필수");
        }

        if (!errors.isEmpty()) {
            // 너무 길면 앞부분만
            String msg = errors.stream().limit(30).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("엑셀 필수값 누락: " + msg);
        }
    }

    /* ===================== existing check ===================== */


    /***
     * 상품마스터 존재여부 검증 및 88코드 기입
     * @param grouped
     * @return
     */
    private Set<String> applyInnerItemCodeOrCollectMissing(Map<String, List<TbMwInboundDelivery>> grouped) {

        // 순서 유지용
        Set<String> missing = new LinkedHashSet<String>();

        // item_code -> master 캐시 (null도 캐시해서 중복 조회 방지)
        Map<String, TbMwItemMaster> cache = new HashMap<String, TbMwItemMaster>();

        for (List<TbMwInboundDelivery> list : grouped.values()) {
            for (TbMwInboundDelivery d : list) {

                String material = safeTrim(d.getItemCode());
                if (ValueUtil.isEmpty(material)) continue; // 필수는 validateRequired에서 걸림

                TbMwItemMaster m = cache.get(material);
                if (!cache.containsKey(material)) {
                    m = tbMwItemMasterService.getItemMasterByMaterial(material);
                    cache.put(material, m);
                }

                if (m == null || ValueUtil.isEmpty(m.getInnerItemCode())) {
                    missing.add(material);
                } else {
                    d.setInnerItemCode(safeTrim(m.getInnerItemCode()));
                }
            }
        }

        return missing;
    }

    /***
     * 기존 import data 거름
     * @param grouped
     * @return
     */
    private Set<String> findExistingJobKeys(Map<String, List<TbMwInboundDelivery>> grouped) {
        // (bl_no, cntr_no) 튜플 IN 조회
        StringBuilder sb = new StringBuilder();
        Map<String, Object> params = new HashMap<String, Object>();

        sb.append("SELECT * \n");
        sb.append("  FROM tb_mw_inbound_job \n");
        sb.append(" WHERE (bl_no, cntr_no) IN ( \n");

        int idx = 0;
        for (String key : grouped.keySet()) {
            String[] parts = key.split("\\|", -1);
            String bl = parts[0];
            String cn = parts[1];

            if (idx > 0) sb.append(",\n");
            sb.append(" ( :bl").append(idx).append(", :cn").append(idx).append(" )");

            params.put("bl" + idx, bl);
            params.put("cn" + idx, cn);
            idx++;
        }

        sb.append("\n )");

        @SuppressWarnings("unchecked")
        List<TbMwInboundJob> rows = this.queryManager.selectListBySql(sb.toString(), params, TbMwInboundJob.class, 0, 0);

        Set<String> existing = new HashSet<String>();
        for (TbMwInboundJob r : rows) {
            String bl = safeTrim(String.valueOf(r.getBlNo()));
            String cn = safeTrim(String.valueOf(r.getCntrNo()));
            existing.add(makeKey(bl, cn));
        }
        return existing;
    }

    /* ===================== build job / defaults ===================== */

    private TbMwInboundJob buildJobFromGroup(List<TbMwInboundDelivery> rows, String actor, Date now) {

        String blNo = safeTrim(rows.get(0).getBlNo());
        String cntrNo = safeTrim(rows.get(0).getCntrNo());

        // inbound_date는 그룹 내 첫 로우 기준(필요하면 min/max로 바꿔도 됨)
        Date inboundDate = rows.get(0).getInboundDate();

        // 총 상품 종수 계산
        int skuQty = (int) rows.stream()
                .map(r -> safeTrim(r.getItemCode()))
                .filter(s -> !ValueUtil.isEmpty(s))
                .distinct()
                .count();

        // 토탈 박스수량 계산
        int totalItemQty = 0;
        for (TbMwInboundDelivery r : rows) {
            totalItemQty += (r.getItemQty() == null ? 0 : r.getItemQty().intValue());
        }

        TbMwInboundJob job = new TbMwInboundJob();
        job.setId(UUID.randomUUID().toString());
        job.setJobNo(makeJobNo(blNo, cntrNo));
        job.setBlNo(blNo);
        job.setCntrNo(cntrNo);
        job.setInboundDate(inboundDate);

        // 상태(기본: 대기)
        job.setJobStatus(0);
        job.setJobStatusDesc("대기");

        job.setJobStartDt(null);
        job.setJobEndDt(null);

        job.setSkuQty(skuQty);
        job.setTotalItemQty(totalItemQty);
        job.setcompletedItemQty(0);
        job.setNgItemQty(0);

        job.setDomainId(WcsConstants.DOMAIN_ID);
        job.setCreatorId(actor);
        job.setUpdaterId(actor);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        return job;
    }

    /***
     * InboundDelivery 데이터 최초 생성 부분
     * @param d 반환 저장 값
     * @param inboundSeq
     * @param actor
     * @param now
     */
    private void applyDefaultsToDelivery(TbMwInboundDelivery d, int inboundSeq, String actor, Date now) {

        // uuid 발번
        if (ValueUtil.isEmpty(d.getId())) d.setId(UUID.randomUUID().toString());

        // inbound_seq는 "순번 변경" 화면에서 수정하니까 우선 1..N 세팅
        if (ValueUtil.isEmpty(d.getInboundSeq())) d.setInboundSeq(String.valueOf(inboundSeq));

        // 아래는 TbMwInboundDelivery 엔티티 기준 NOT NULL 방어용 기본값
        if (ValueUtil.isEmpty(d.getLcId())) d.setLcId("DEFAULT");
        if (ValueUtil.isEmpty(d.getLcNm())) d.setLcNm("DEFAULT");

        if (ValueUtil.isEmpty(d.getInvoice())) d.setInvoice(""); // 엔티티상 nullable=false라 방어

        if (ValueUtil.isEmpty(d.getItemType())) {
            // 엑셀의 itme_type을 item_type으로 매핑해 넣는 구조면 프론트에서 이미 세팅될 것.
            // 혹시 누락되면 방어:
            d.setItemType("UNKNOWN");
        }

        if (ValueUtil.isEmpty(d.getItemDesc())) d.setItemDesc("-");

        if (d.getItemCbm() == null) d.setItemCbm(0.0);
        if (d.getTotalCbm() == null) d.setTotalCbm(0.0);
        if (ValueUtil.isEmpty(d.getItemPriority())) d.setItemPriority("0");

        if (d.getInboundStatus() == null) d.setInboundStatus(InboundStatus.READY.value());

        // reg_time 같은 문자열 컬럼은 테이블에 있다면 now로 채우는 편
        if (ValueUtil.isEmpty(d.getRegId())) d.setRegId(actor);
        if (ValueUtil.isEmpty(d.getRegTime())) d.setRegTime(String.valueOf(now.getTime()));

        d.setCreatedAt(now);
        d.setManualFlag(false);

        d.setPassQty(0);
        d.setNgQty(0);

        // ElidomStampHook 컬럼은 SQL에서 now()/actor로 저장할 예정이라 여기서는 생략 가능
    }

    /* ===================== bulk insert ===================== */

    private int bulkInsertJobs(List<TbMwInboundJob> jobs, int chunkSize) {
        if (jobs == null || jobs.isEmpty()) return 0;

        int inserted = 0;
        for (int from = 0; from < jobs.size(); from += chunkSize) {
            int to = Math.min(from + chunkSize, jobs.size());
            List<TbMwInboundJob> chunk = jobs.subList(from, to);

            StringBuilder sb = new StringBuilder();
            Map<String, Object> params = new HashMap<String, Object>();

            sb.append("INSERT INTO tb_mw_inbound_job ( \n");
            sb.append("  id, job_no, bl_no, invoice, cntr_no, inbound_date, \n");
            sb.append("  job_status_desc, job_status, job_start_dt, job_end_dt, \n");
            sb.append("  sku_qty, total_item_qty, completed_item_qty, ng_item_qty, \n");
            sb.append("  attribute_1, attribute_2, attribute_3, attribute_4, \n");
            sb.append("  creator_id, updater_id, created_at, updated_at, domain_id \n");
            sb.append(") VALUES \n");

            for (int i = 0; i < chunk.size(); i++) {
                TbMwInboundJob j = chunk.get(i);
                if (i > 0) sb.append(",\n");

                sb.append(" ( ")
                        .append(":id").append(i).append(", ")
                        .append(":jobNo").append(i).append(", ")
                        .append(":blNo").append(i).append(", ")
                        .append(":invoice").append(i).append(", ")
                        .append(":cntrNo").append(i).append(", ")
                        .append(":inboundDate").append(i).append(", ")
                        .append(":jobStatusDesc").append(i).append(", ")
                        .append(":jobStatus").append(i).append(", ")
                        .append(":jobStartDt").append(i).append(", ")
                        .append(":jobEndDt").append(i).append(", ")
                        .append(":skuQty").append(i).append(", ")
                        .append(":totalItemQty").append(i).append(", ")
                        .append(":completedItemQty").append(i).append(", ")
                        .append(":ngItemQty").append(i).append(", ")
                        .append(":attr1").append(i).append(", ")
                        .append(":attr2").append(i).append(", ")
                        .append(":attr3").append(i).append(", ")
                        .append(":attr4").append(i).append(", ")
                        .append(":creatorId").append(i).append(", ")
                        .append(":updaterId").append(i).append(", ")
                        .append(":createdAt").append(i).append(", ")
                        .append(":updatedAt").append(i).append(", ")
                        .append(":domainId").append(i)
                        .append(" )");

                params.put("id" + i, j.getId());
                params.put("jobNo" + i, j.getJobNo());
                params.put("blNo" + i, j.getBlNo());
                params.put("invoice" + i, j.getInvoice());
                params.put("cntrNo" + i, j.getCntrNo());
                params.put("inboundDate" + i, j.getInboundDate());
                params.put("jobStatusDesc" + i, j.getJobStatusDesc());
                params.put("jobStatus" + i, j.getJobStatus());
                params.put("jobStartDt" + i, j.getJobStartDt());
                params.put("jobEndDt" + i, j.getJobEndDt());
                params.put("skuQty" + i, j.getSkuQty());
                params.put("totalItemQty" + i, j.getTotalItemQty());
                params.put("completedItemQty" + i, j.getcompletedItemQty());
                params.put("ngItemQty" + i, j.getNgItemQty());

                params.put("attr1" + i, j.getAttribute1());
                params.put("attr2" + i, j.getAttribute2());
                params.put("attr3" + i, j.getAttribute3());
                params.put("attr4" + i, j.getAttribute4());

                params.put("creatorId" + i, j.getCreatorId());
                params.put("updaterId" + i, j.getUpdaterId());
                params.put("createdAt" + i, j.getCreatedAt());
                params.put("updatedAt" + i, j.getUpdatedAt());
                params.put("domainId" + i, j.getDomainId());
            }

            queryManager.executeBySql(sb.toString(), params);
            inserted += chunk.size();
        }

        return inserted;
    }

    private int bulkInsertDeliveries(List<TbMwInboundDelivery> rows, int chunkSize) {
        if (rows == null || rows.isEmpty()) return 0;

        int inserted = 0;
        for (int from = 0; from < rows.size(); from += chunkSize) {
            int to = Math.min(from + chunkSize, rows.size());
            List<TbMwInboundDelivery> chunk = rows.subList(from, to);

            StringBuilder sb = new StringBuilder();
            Map<String, Object> params = new HashMap<String, Object>();

            sb.append("INSERT INTO tb_mw_inbound_delivery ( \n");
            sb.append("  id, domain_id, lc_id, lc_nm, inbound_seq, cntr_no, dock_id, cust_id, cust_nm, \n");
            sb.append("  bl_no, invoice, inbound_date, remark, lot_no, box_barcode, real_order_no, item_order_no, \n");
            sb.append("  item_code, owner_item_code, inner_item_code, item_name, item_type, item_desc, \n");
            sb.append("  item_qty, item_cbm, total_cbm, item_priority, reg_id, reg_time, inbound_status, \n");
            sb.append("  start_datetime, complete_datetime, manual_flag, pass_qty, ng_qty, \n");
            sb.append("  creator_id, updater_id, created_at, updated_at \n");
            sb.append(") VALUES \n");

            for (int i = 0; i < chunk.size(); i++) {
                TbMwInboundDelivery d = chunk.get(i);
                if (i > 0) sb.append(",\n");

                sb.append(" ( ")
                        .append(":id").append(i).append(", ")
                        .append(":domain_id").append(i).append(", ")
                        .append(":lcId").append(i).append(", ")
                        .append(":lcNm").append(i).append(", ")
                        .append(":inboundSeq").append(i).append(", ")
                        .append(":cntrNo").append(i).append(", ")
                        .append(":dockId").append(i).append(", ")
                        .append(":custId").append(i).append(", ")
                        .append(":custNm").append(i).append(", ")
                        .append(":blNo").append(i).append(", ")
                        .append(":invoice").append(i).append(", ")
                        .append(":inboundDate").append(i).append(", ")
                        .append(":remark").append(i).append(", ")
                        .append(":lotNo").append(i).append(", ")
                        .append(":boxBarcode").append(i).append(", ")
                        .append(":realOrderNo").append(i).append(", ")
                        .append(":itemOrderNo").append(i).append(", ")
                        .append(":itemCode").append(i).append(", ")
                        .append(":ownerItemCode").append(i).append(", ")
                        .append(":innerItemCode").append(i).append(", ")
                        .append(":itemName").append(i).append(", ")
                        .append(":itemType").append(i).append(", ")
                        .append(":itemDesc").append(i).append(", ")
                        .append(":itemQty").append(i).append(", ")
                        .append(":itemCbm").append(i).append(", ")
                        .append(":totalCbm").append(i).append(", ")
                        .append(":itemPriority").append(i).append(", ")
                        .append(":regId").append(i).append(", ")
                        .append(":regTime").append(i).append(", ")
                        .append(":inboundStatus").append(i).append(", ")
                        .append(":startDatetime").append(i).append(", ")
                        .append(":completeDatetime").append(i).append(", ")
                        .append(":manualFlag").append(i).append(", ")
                        .append(":passQty").append(i).append(", ")
                        .append(":ngQty").append(i).append(", ")
                        .append(":creatorId").append(i).append(", ")
                        .append(":updaterId").append(i).append(", ")
                        .append(":createdAt").append(i).append(", ")
                        .append(":updatedAt").append(i)
                        .append(" )");

                params.put("id" + i, d.getId());
                params.put("domain_id" + i, 7L);
                params.put("lcId" + i, d.getLcId());
                params.put("lcNm" + i, d.getLcNm());
                params.put("inboundSeq" + i, d.getInboundSeq());
                params.put("cntrNo" + i, d.getCntrNo());
                params.put("dockId" + i, d.getDockId());
                params.put("custId" + i, d.getCustId());
                params.put("custNm" + i, d.getCustNm());
                params.put("blNo" + i, d.getBlNo());
                params.put("invoice" + i, d.getInvoice());
                params.put("inboundDate" + i, d.getInboundDate());
                params.put("remark" + i, d.getRemark());
                params.put("lotNo" + i, d.getLotNo());
                params.put("boxBarcode" + i, d.getBoxBarcode());
                params.put("realOrderNo" + i, d.getRealOrderNo());
                params.put("itemOrderNo" + i, d.getItemOrderNo());
                params.put("itemCode" + i, d.getItemCode());
                params.put("ownerItemCode" + i, d.getOwnerItemCode());
                params.put("innerItemCode" + i, d.getInnerItemCode());
                params.put("itemName" + i, d.getItemName());
                params.put("itemType" + i, d.getItemType());
                params.put("itemDesc" + i, d.getItemDesc());
                params.put("itemQty" + i, d.getItemQty());
                params.put("itemCbm" + i, d.getItemCbm());
                params.put("totalCbm" + i, d.getTotalCbm());
                params.put("itemPriority" + i, d.getItemPriority());
                params.put("regId" + i, d.getRegId());
                params.put("regTime" + i, d.getRegTime());
                params.put("inboundStatus" + i, d.getInboundStatus());
                params.put("startDatetime" + i, d.getStartDatetime());
                params.put("completeDatetime" + i, d.getCompleteDatetime());
                params.put("manualFlag" + i, d.isManualFlag());
                params.put("passQty" + i, d.getPassQty());
                params.put("ngQty" + i, d.getNgQty());

                // Elidom stamp
                params.put("creatorId" + i, safeTrim(d.getCreatorId()));
                params.put("updaterId" + i, safeTrim(d.getUpdaterId()));
                params.put("createdAt" + i, d.getCreatedAt());
                params.put("updatedAt" + i, d.getUpdatedAt());
            }

            queryManager.executeBySql(sb.toString(), params);
            inserted += chunk.size();
        }

        return inserted;
    }

    /* ===================== utils ===================== */

    private String makeKey(String blNo, String cntrNo) {
        return safeTrim(blNo) + "|" + safeTrim(cntrNo);
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String makeJobNo(String blNo, String cntrNo) {
        // 길이 이슈 있으면 UUID로 간단히
        String base = "INB-" + safeTrim(blNo) + "-" + safeTrim(cntrNo);
        if (base.length() > 80) base = "INB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return base + "-" + (System.currentTimeMillis() % 100000);
    }

    private String resolveActorId() {
        // 프로젝트에 맞는 current user 가져오는 방식으로 교체하면 됨
        return "system";
    }
}