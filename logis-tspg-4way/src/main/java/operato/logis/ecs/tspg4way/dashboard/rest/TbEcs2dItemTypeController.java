package operato.logis.ecs.tspg4way.dashboard.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import operato.logis.ecs.tspg4way.dashboard.entity.TbEcs2dItemType;
import operato.logis.ecs.tspg4way.dashboard.service.impl.TbEcs2dItemTypeService;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 4-Way Shuttle 설비 타입 Controller
 */
@RestController
@RequestMapping(value = "/rest/shuttle_equipment_types", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "4-Way Shuttle 설비 타입 API")
public class TbEcs2dItemTypeController {

    @Autowired
    private TbEcs2dItemTypeService equipmentTypeService;

    /**
     * 센터별 설비 타입 목록 조회
     */
    @GetMapping("/{lcId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dItemType> getTypes(@PathVariable String lcId) {
        return this.equipmentTypeService.getTypesByLcId(lcId);
    }

    /**
     * 카테고리별 설비 타입 목록 조회
     */
    @GetMapping("/{lcId}/category/{category}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dItemType> getTypesByCategory(@PathVariable String lcId, @PathVariable String category) {
        return this.equipmentTypeService.getTypesByCategory(lcId, category);
    }

    /**
     * 설비 타입 단건 조회
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItemType getType(@PathVariable String id) {
        return this.equipmentTypeService.getType(id);
    }

    /**
     * 설비 타입 생성
     */
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItemType createType(@RequestBody TbEcs2dItemType type) {
        return this.equipmentTypeService.createType(type);
    }

    /**
     * 설비 타입 수정
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItemType updateType(@RequestBody TbEcs2dItemType type) {
        return this.equipmentTypeService.updateType(type);
    }

    /**
     * 설비 타입 아이콘 업데이트
     */
    @PostMapping("/update_icon")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItemType updateTypeIcon(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String iconData2d = params.get("icon_data_2d");
        return this.equipmentTypeService.updateTypeIcon(id, iconData2d);
    }

    /**
     * 설비 타입 삭제
     */
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean deleteType(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        this.equipmentTypeService.deleteType(id);
        return true;
    }

    /**
     * 기본 설비 타입 초기화
     */
    @PostMapping("/initialize")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean initializeDefaultTypes(@RequestBody Map<String,Object> requestBody) {
        String lcId = (String) requestBody.get("lc_id");
        this.equipmentTypeService.initializeDefaultTypes(lcId);
        return true;
    }

    /**
     * DEFAULT 마스터 → targetLcId 복제
     * Body: { "target_lc_id": "LC_13" }
     */
    @PostMapping("/clone")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> cloneToCenter(@RequestBody Map<String, Object> requestBody) {
        String targetLcId = (String) requestBody.get("target_lc_id");
        int count = this.equipmentTypeService.cloneToCenter(targetLcId);
        Map<String, Object> result = new HashMap<>();
        result.put("cloned_count", count);
        result.put("target_lc_id", targetLcId);
        return result;
    }

    /**
     * 일괄 저장 (Upsert) - 프론트 로컬 SVG + 기본 Config를 한번에 등록
     * Body: { "lc_id": "DEFAULT", "types": [...] }
     */
    @PostMapping("/save_batch")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dItemType> saveBatch(@RequestBody Map<String, Object> requestBody) {
        String lcId = (String) requestBody.get("lc_id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawTypes = (List<Map<String, Object>>) requestBody.get("types");

        List<TbEcs2dItemType> types = rawTypes.stream().map(raw -> {
            TbEcs2dItemType t = new TbEcs2dItemType();
            t.setTypeCode((String) raw.get("type_code"));
            t.setTypeName((String) raw.get("type_name"));
            t.setCategory((String) raw.get("category"));
            t.setLayerType((String) raw.get("layer_type"));
            t.setIconData2d((String) raw.get("icon_data_2d"));
            t.setIconFileName((String) raw.get("icon_file_name"));
            t.setDescription((String) raw.get("description"));
            if (raw.get("real_eq_type_num") != null) {
                t.setRealEqTypeNum(((Number) raw.get("real_eq_type_num")).intValue());
            }
            if (raw.get("has_cargo") != null) t.setHasCargo((Boolean) raw.get("has_cargo"));
            if (raw.get("has_inventory") != null) t.setHasInventory((Boolean) raw.get("has_inventory"));
            if (raw.get("default_width") != null) t.setDefaultWidth(((Number) raw.get("default_width")).intValue());
            if (raw.get("default_height") != null) t.setDefaultHeight(((Number) raw.get("default_height")).intValue());
            if (raw.get("sort_order") != null) t.setSortOrder(((Number) raw.get("sort_order")).intValue());
            t.setRotatable(raw.get("rotatable") == null ? true : (Boolean) raw.get("rotatable"));
            t.setResizable(raw.get("resizable") == null ? true : (Boolean) raw.get("resizable"));
            t.setShowStatus(raw.get("show_status") == null ? false : (Boolean) raw.get("show_status"));
            t.setShowPopup(raw.get("show_popup") == null ? true : (Boolean) raw.get("show_popup"));
            t.setIsActive(true);
            return t;
        }).collect(java.util.stream.Collectors.toList());

        return this.equipmentTypeService.saveBatch(lcId, types);
    }
}
