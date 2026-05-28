package operato.logis.ecs.base.ecs.dashboard.rest;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import operato.logis.ecs.base.ecs.dashboard.entity.TbEcs2dItem;
import operato.logis.ecs.base.ecs.dashboard.service.impl.TbEcs2dItemService;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.util.ThrowUtil;

/**
 * 4-Way Shuttle 설비 레이아웃 Controller
 */
@RestController
@RequestMapping(value = "/rest/tb_ecs_2d_item", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "4-Way Shuttle 설비 레이아웃 API")
public class TbEcs2dItemController {

    private static final Logger logger = LoggerFactory.getLogger(TbEcs2dItemController.class);

    @Autowired
    private TbEcs2dItemService equipmentLayoutService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 숫자 파라미터 안전하게 추출
     */
    private Double getDoubleParam(Map<String, Object> params, String key, boolean required) {
        Object value = params.get(key);
        if (value == null) {
            if (required) {
                throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, key);
            }
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw ThrowUtil.newValidationErrorWithNoLog("Invalid type for parameter: " + key);
    }

    private String getStringParam(Map<String, Object> params, String key, boolean required) {
        Object value = params.get(key);
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            if (required) {
                throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, key);
            }
            return null;
        }
        return value.toString();
    }

    private Boolean getBooleanParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 페이지별 레이아웃 목록 조회
     */
    @GetMapping("/{lcId}/{pageId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dItem> getLayouts(@PathVariable String lcId, @PathVariable String pageId) {
        return this.equipmentLayoutService.getLayoutsByPageId(lcId, pageId);
    }

    /**
     * 레이아웃 단건 조회
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem getLayout(@PathVariable String id) {
        return this.equipmentLayoutService.getLayout(id);
    }

    /**
     * 레이아웃 생성 (설비 배치)
     */
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem createLayout(@RequestBody TbEcs2dItem layout) {
        return this.equipmentLayoutService.createLayout(layout);
    }

    /**
     * 레이아웃 수정
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem updateLayout(@RequestBody TbEcs2dItem layout) {
        return this.equipmentLayoutService.updateLayout(layout);
    }

    /**
     * 레이아웃 위치 수정 (드래그)
     */
    @PostMapping("/update_position")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem updateLayoutPosition(@RequestBody Map<String, Object> params) {
        String id = this.getStringParam(params, "id", true);
        Double posX = this.getDoubleParam(params, "pos_x", true);
        Double posY = this.getDoubleParam(params, "pos_y", true);

        logger.debug("Update layout position - id: {}, posX: {}, posY: {}", id, posX, posY);
        return this.equipmentLayoutService.updateLayoutPosition(id, posX, posY);
    }

    /**
     * 레이아웃 크기 수정
     */
    @PostMapping("/update_size")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem updateLayoutSize(@RequestBody Map<String, Object> params) {
        String id = this.getStringParam(params, "id", true);
        Double width = this.getDoubleParam(params, "width", true);
        Double height = this.getDoubleParam(params, "height", true);

        // 유효성 검증
        if (width <= 0 || height <= 0) {
            throw ThrowUtil.newValidationErrorWithNoLog("Width and height must be positive values");
        }

        logger.debug("Update layout size - id: {}, width: {}, height: {}", id, width, height);
        return this.equipmentLayoutService.updateLayoutSize(id, width, height);
    }

    /**
     * 레이아웃 변형 수정 (회전, 스케일, 플립)
     */
    @PostMapping("/update_transform")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem updateLayoutTransform(@RequestBody Map<String, Object> params) {
        String id = this.getStringParam(params, "id", true);
        Double rotation = this.getDoubleParam(params, "rotation", false);
        Double scaleX = this.getDoubleParam(params, "scale_x", false);
        Double scaleY = this.getDoubleParam(params, "scale_y", false);
        Boolean flipH = this.getBooleanParam(params, "flip_h");
        Boolean flipV = this.getBooleanParam(params, "flip_v");

        logger.debug("Update layout transform - id: {}, rotation: {}, scaleX: {}, scaleY: {}, flipH: {}, flipV: {}",
            id, rotation, scaleX, scaleY, flipH, flipV);
        return this.equipmentLayoutService.updateLayoutTransform(id, rotation, scaleX, scaleY, flipH, flipV);
    }

    /**
     * 레이아웃 삭제
     */
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean deleteLayout(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        this.equipmentLayoutService.deleteLayout(id);
        return true;
    }

    /**
     * 페이지 전체 레이아웃 일괄 저장
     */
    @PostMapping("/save_all")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean saveLayouts(@RequestBody Map<String, Object> params) {
        String lcId = this.getStringParam(params, "lc_id", true);
        String pageId = this.getStringParam(params, "page_id", true);
        Object layoutsObj = params.get("layouts");

        if (layoutsObj == null) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "layouts");
        }

        try {
            List<TbEcs2dItem> layouts = objectMapper.convertValue(
                layoutsObj,
                new TypeReference<List<TbEcs2dItem>>() {}
            );

            logger.info("Saving {} layouts for page {} in lcId {}", layouts.size(), pageId, lcId);
            this.equipmentLayoutService.saveLayouts(lcId, pageId, layouts);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("Failed to parse layouts: {}", e.getMessage());
            throw ThrowUtil.newValidationErrorWithNoLog("Invalid layouts format");
        }
    }

    /**
     * 레이아웃 일괄 업데이트
     */
    @PostMapping("/batch_update")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean batchUpdateLayouts(@RequestBody List<TbEcs2dItem> layouts) {
        this.equipmentLayoutService.batchUpdateLayouts(layouts);
        return true;
    }

    /**
     * 레이아웃에 실운영 설비 ID 매핑
     */
    @PostMapping("/update_real_eq_mapping")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem updateRealEqMapping(@RequestBody Map<String, Object> params) {
        String id = this.getStringParam(params, "id", true);
        String realEqId = this.getStringParam(params, "real_eq_id", false);
        String realEqType = this.getStringParam(params, "real_eq_type", false);

        logger.info("Update real equipment mapping - layoutId: {}, realEqId: {}, realEqType: {}", id, realEqId, realEqType);
        return this.equipmentLayoutService.updateRealEqMapping(id, realEqId, realEqType);
    }

    /**
     * 레이아웃의 실운영 설비 ID 매핑 해제
     */
    @PostMapping("/clear_real_eq_mapping")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem clearRealEqMapping(@RequestBody Map<String, Object> params) {
        String id = this.getStringParam(params, "id", true);
        logger.info("Clear real equipment mapping - layoutId: {}", id);
        return this.equipmentLayoutService.clearRealEqMapping(id);
    }

    /**
     * 대시보드용 레이아웃 조회 (실시간 상태 결합)
     */
    @GetMapping("/{lcId}/{pageId}/with_real_status")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Map> getLayoutsWithRealStatus(@PathVariable String lcId, @PathVariable String pageId) {
        return this.equipmentLayoutService.getLayoutsWithRealStatus(lcId, pageId);
    }

    /**
     * realEqId로 레이아웃 조회
     */
    @GetMapping("/{lcId}/{pageId}/by_real_eq/{realEqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dItem getLayoutByRealEqId(
            @PathVariable String lcId,
            @PathVariable String pageId,
            @PathVariable String realEqId) {
        return this.equipmentLayoutService.getLayoutByRealEqId(lcId, pageId, realEqId);
    }

    /**
     * 특정 페이지에서 realEqId가 매핑된 레이아웃 목록 조회
     */
    @GetMapping("/{lcId}/{pageId}/mapped")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dItem> getMappedLayouts(@PathVariable String lcId, @PathVariable String pageId) {
        return this.equipmentLayoutService.getMappedLayouts(lcId, pageId);
    }
}
