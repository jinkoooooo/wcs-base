package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.entity.TbEcs2dPage;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.service.impl.TbEcs2dPageService;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 4-Way Shuttle 레이아웃 페이지 Controller
 */
@RestController
@RequestMapping(value = "/rest/tb_ecs_2d_page", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "4-Way Shuttle 레이아웃 페이지 API")
public class TbEcs2dPageController {

    @Autowired
    private TbEcs2dPageService layoutPageService;

    /**
     * 센터별 페이지 목록 조회
     */
    @GetMapping("/{lcId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dPage> getPages(@PathVariable String lcId) {
        return this.layoutPageService.getActivePagesByLcId(lcId);
    }

    /**
     * 설비그룹별 페이지 목록 조회
     */
    @GetMapping("/{lcId}/eq_group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dPage> getPagesByEqGroup(
            @PathVariable String lcId,
            @PathVariable String eqGroupId) {
        return this.layoutPageService.getPagesByEqGroup(lcId, eqGroupId);
    }

    /**
     * 설비그룹 + 층별 페이지 목록 조회
     */
    @GetMapping("/{lcId}/eq_group/{eqGroupId}/floor/{floor}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dPage> getPagesByEqGroupAndFloor(
            @PathVariable String lcId,
            @PathVariable String eqGroupId,
            @PathVariable Integer floor) {
        return this.layoutPageService.getPagesByEqGroupAndFloor(lcId, eqGroupId, floor);
    }

    /**
     * 설비그룹별 층 목록 조회
     */
    @GetMapping("/{lcId}/eq_group/{eqGroupId}/floors")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Integer> getFloorsByEqGroup(
            @PathVariable String lcId,
            @PathVariable String eqGroupId) {
        return this.layoutPageService.getFloorsByEqGroup(lcId, eqGroupId);
    }

    /**
     * 페이지 단건 조회
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage getPage(@PathVariable String id) {
        return this.layoutPageService.getPage(id);
    }

    /**
     * 페이지 생성
     */
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage createPage(@RequestBody TbEcs2dPage page) {
        return this.layoutPageService.createPage(page);
    }

    /**
     * 페이지 수정
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage updatePage(@RequestBody TbEcs2dPage page) {
        return this.layoutPageService.updatePage(page);
    }

    /**
     * 페이지 이름 수정
     */
    @PostMapping("/update_name")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage updatePageName(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        String pageName = (String) params.get("page_name");
        return this.layoutPageService.updatePageName(id, pageName);
    }

    /**
     * 페이지 캔버스 설정 수정
     */
    @PostMapping("/update_canvas")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage updatePageCanvas(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer canvasWidth = params.get("canvas_width") != null ? ((Number) params.get("canvas_width")).intValue() : null;
        Integer canvasHeight = params.get("canvas_height") != null ? ((Number) params.get("canvas_height")).intValue() : null;
        String backgroundColor = (String) params.get("background_color");
        return this.layoutPageService.updatePageCanvas(id, canvasWidth, canvasHeight, backgroundColor);
    }

    /**
     * 페이지 삭제
     */
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean deletePage(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        this.layoutPageService.deletePage(id);
        return true;
    }

    /**
     * 페이지 순서 변경
     */
    @PostMapping("/update_index")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Boolean updatePageIndex(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer newIndex = ((Number) params.get("page_index")).intValue();
        this.layoutPageService.updatePageIndex(id, newIndex);
        return true;
    }

    /**
     * 페이지 복사 (층 복사용)
     * 원본 페이지의 모든 레이아웃을 새 페이지에 복사
     *
     * @param params sourcePageId: 원본 페이지 ID
     *               newPageName: 새 페이지 이름 (예: "2층")
     *               newFloorLevel: 새 페이지 층 번호
     * @return 생성된 새 페이지
     */
    @PostMapping("/copy")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage copyPage(@RequestBody Map<String, Object> params) {
        String sourcePageId = (String) params.get("source_page_id");
        String newPageName = (String) params.get("new_page_name");
        Integer newFloorLevel = params.get("new_floor_level") != null
            ? ((Number) params.get("new_floor_level")).intValue() : null;

        return this.layoutPageService.copyPage(sourcePageId, newPageName, newFloorLevel);
    }

    /**
     * 페이지 설비그룹 매핑 업데이트
     */
    @PostMapping("/update_eq_group")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage updatePageEqGroup(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String eqGroupId = params.get("eq_group_id");
        return this.layoutPageService.updatePageEqGroup(id, eqGroupId);
    }

    /**
     * 페이지 설비그룹 매핑 해제
     */
    @PostMapping("/clear_eq_group")
    @Transactional(rollbackFor = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public TbEcs2dPage clearPageEqGroup(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        return this.layoutPageService.clearPageEqGroup(id);
    }

    /**
     * 설비그룹 없는 페이지 목록 조회 (매핑되지 않은 페이지)
     */
    @GetMapping("/{lcId}/unmapped")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEcs2dPage> getUnmappedPages(@PathVariable String lcId) {
        return this.layoutPageService.getActivePagesByLcId(lcId);
    }
}
