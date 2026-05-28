package operato.logis.ecs.base.ecs.dashboard.service.impl;

import operato.logis.ecs.base.ecs.dashboard.entity.TbEcs2dItemType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

/** 4-Way Shuttle 설비 타입 Service */
@Service
public class TbEcs2dItemTypeService extends AbstractQueryService {

    /** 센터별 전체 설비 타입 목록 조회 */
    @Transactional(readOnly = true)
    public List<TbEcs2dItemType> getTypesByLcId(String lcId) {
        String sql = "SELECT * FROM tb_ecs_2d_item_type WHERE lc_id = :lcId AND is_active = true ORDER BY sort_order ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId", lcId);
        return this.queryManager.selectListBySql(sql, params, TbEcs2dItemType.class, 0, 0);
    }

    /** 카테고리별 설비 타입 목록 조회 */
    @Transactional(readOnly = true)
    public List<TbEcs2dItemType> getTypesByCategory(String lcId, String category) {
        String sql = "SELECT * FROM tb_ecs_2d_item_type WHERE lc_id = :lcId AND category = :category AND is_active = true ORDER BY sort_order ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,category", lcId, category);
        return this.queryManager.selectListBySql(sql, params, TbEcs2dItemType.class, 0, 0);
    }

    /** 설비 타입 단건 조회 */
    @Transactional(readOnly = true)
    public TbEcs2dItemType getType(String id) {
        return this.queryManager.select(TbEcs2dItemType.class, id);
    }

    /** 설비 타입 코드로 조회 */
    @Transactional(readOnly = true)
    public TbEcs2dItemType getTypeByCode(String lcId, String typeCode) {
        Map<String, Object> params = ValueUtil.newMap("lcId,typeCode", lcId, typeCode);
        return this.queryManager.selectByCondition(TbEcs2dItemType.class, params);
    }

    /** 설비 타입 생성 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItemType createType(TbEcs2dItemType type) {
        this.queryManager.insert(type);
        return type;
    }

    /** 설비 타입 수정 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItemType updateType(TbEcs2dItemType type) {
        this.queryManager.update(type);
        return type;
    }

    /** 설비 타입 아이콘 업데이트 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItemType updateTypeIcon(String id, String iconData2d) {
        TbEcs2dItemType type = this.getType(id);
        if (type != null) {
            type.setIconData2d(iconData2d);
            this.queryManager.update(type, "iconData2d");
        }
        return type;
    }

    /** 설비 타입 삭제 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(String id) {
        TbEcs2dItemType type = this.getType(id);
        if (type != null) {
            this.queryManager.delete(type);
        }
    }

    /**
     * 기본 설비 타입 초기화 (샘플 데이터)
     * {typeCode, typeName, category, layerType, realEqTypeNum, hasCargo, hasInventory}
     */
    @Transactional(rollbackFor = Exception.class)
    public void initializeDefaultTypes(String lcId) {
        Object[][] defaultTypes = {
                // typeCode, typeName, category, layerType, realEqTypeNum, hasCargo, hasInventory
                { "CONVEYOR", "컨베이어", "이동설비", "static", 21, true, false },
                { "LIFTER", "리프터", "이동설비", "static", 21, true, false },
                { "BCR", "바코드리더", "검증설비", "static", null, false, false },
                { "RACK", "랙", "보관설비", "static", 11, false, true },
                { "SHUTTLE", "셔틀", "이동설비", "dynamic", 22, true, false },
                { "STV", "STV", "이동설비", "dynamic", 22, true, false },
                { "CRANE", "크레인", "이동설비", "dynamic", null, false, false },
                { "BUFFER", "버퍼", "보관설비", "static", null, false, false },
                { "WORKSTATION", "작업대", "작업설비", "static", null, false, false },
                { "GATE", "게이트", "기타", "static", null, false, false },
                { "PILLAR", "기둥", "구조물", "overlay", null, false, false },
                { "RAIL_LANE", "레일", "구조물", "overlay", null, false, false },
                { "CARGO", "화물", "화물", "overlay", null, true, false },
        };

        int sortOrder = 0;
        for (Object[] typeInfo : defaultTypes) {
            String typeCode = (String) typeInfo[0];
            TbEcs2dItemType existingType = this.getTypeByCode(lcId, typeCode);
            if (existingType == null) {
                TbEcs2dItemType type = new TbEcs2dItemType();
                type.setLcId(lcId);
                type.setTypeCode(typeCode);
                type.setTypeName((String) typeInfo[1]);
                type.setCategory((String) typeInfo[2]);
                type.setLayerType((String) typeInfo[3]);
                type.setRealEqTypeNum((Integer) typeInfo[4]);
                type.setHasCargo((Boolean) typeInfo[5]);
                type.setHasInventory((Boolean) typeInfo[6]);

                type.setDefaultWidth(100);
                type.setDefaultHeight(100);
                type.setRotatable(true);
                type.setResizable(true);
                type.setShowStatus(false);
                type.setShowPopup(true);
                type.setSortOrder(sortOrder++);
                type.setIsActive(true);
                type.setIconFileName(typeCode.toLowerCase() + ".svg");
                this.createType(type);
            } else {
                if (existingType.getIsActive() == null || !existingType.getIsActive()) {
                    existingType.setIsActive(true);
                    this.queryManager.update(existingType, "isActive");
                }
                if (existingType.getSortOrder() == null) {
                    existingType.setSortOrder(sortOrder);
                    this.queryManager.update(existingType, "sortOrder");
                }
                // layerType, realEqTypeNum 등 신규 필드가 비어있으면 보정
                if (existingType.getLayerType() == null) {
                    existingType.setLayerType((String) typeInfo[3]);
                    existingType.setRealEqTypeNum((Integer) typeInfo[4]);
                    existingType.setHasCargo((Boolean) typeInfo[5]);
                    existingType.setHasInventory((Boolean) typeInfo[6]);
                    this.queryManager.update(existingType, "layerType,realEqTypeNum,hasCargo,hasInventory");
                }
                sortOrder++;
            }
        }
    }

    /**
     * DEFAULT 마스터 → targetLcId 복제(Clone Insert)
     * - DEFAULT에 있는 모든 타입을 targetLcId로 복제
     * - 이미 존재하는 typeCode는 건너뜀
     */
    @Transactional(rollbackFor = Exception.class)
    public int cloneToCenter(String targetLcId) {
        if (targetLcId == null || targetLcId.isEmpty()) {
            throw new IllegalArgumentException("targetLcId is required");
        }

        List<TbEcs2dItemType> masterTypes = this.getTypesByLcId("DEFAULT");
        int clonedCount = 0;

        for (TbEcs2dItemType master : masterTypes) {
            TbEcs2dItemType existing = this.getTypeByCode(targetLcId, master.getTypeCode());
            if (existing == null) {
                TbEcs2dItemType clone = new TbEcs2dItemType();
                clone.setLcId(targetLcId);
                clone.setTypeCode(master.getTypeCode());
                clone.setTypeName(master.getTypeName());
                clone.setCategory(master.getCategory());
                clone.setLayerType(master.getLayerType());
                clone.setRealEqTypeNum(master.getRealEqTypeNum());
                clone.setHasCargo(master.getHasCargo());
                clone.setHasInventory(master.getHasInventory());
                clone.setIconData2d(master.getIconData2d());
                clone.setIconUrl2d(master.getIconUrl2d());
                clone.setIconFileName(master.getIconFileName());
                clone.setDefaultWidth(master.getDefaultWidth());
                clone.setDefaultHeight(master.getDefaultHeight());
                clone.setRotatable(master.getRotatable());
                clone.setResizable(master.getResizable());
                clone.setShowStatus(master.getShowStatus());
                clone.setShowPopup(master.getShowPopup());
                clone.setSortOrder(master.getSortOrder());
                clone.setIsActive(true);
                clone.setDescription(master.getDescription());
                this.createType(clone);
                clonedCount++;
            }
        }

        return clonedCount;
    }

    /**
     * 일괄 저장 (Upsert)
     * - 프론트에서 로컬 SVG + 기본 Config를 묶어 일괄 등록할 때 사용
     * - lcId + typeCode 기준으로 존재 여부 확인 후 insert/update
     */
    @Transactional(rollbackFor = Exception.class)
    public List<TbEcs2dItemType> saveBatch(String lcId, List<TbEcs2dItemType> types) {
        for (TbEcs2dItemType incoming : types) {
            incoming.setLcId(lcId);
            TbEcs2dItemType existing = this.getTypeByCode(lcId, incoming.getTypeCode());
            if (existing == null) {
                if (incoming.getIsActive() == null) incoming.setIsActive(true);
                this.queryManager.insert(incoming);
            } else {
                incoming.setId(existing.getId());
                this.queryManager.update(incoming);
            }
        }
        return this.getTypesByLcId(lcId);
    }
}
