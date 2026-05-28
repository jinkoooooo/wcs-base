package operato.logis.ecs.base.ecs.dashboard.rest;

import java.util.List;

import operato.logis.ecs.base.ecs.entity.TbEqGroupMst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import operato.logis.ecs.base.ecs.dashboard.service.impl.TbEqGroupMstService;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 설비 그룹 마스터 Controller (대시보드용)
 * 대시보드에서 설비 그룹 계층 선택을 위한 API
 */
@RestController
@RequestMapping(value = "/rest/tb_eq_group_mst", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "설비 그룹 마스터 API")
public class TbEqGroupMstController {

    @Autowired
    private TbEqGroupMstService eqGroupMstService;

    /**
     * 센터별 설비 그룹 목록 조회
     */
    @GetMapping("/{lcId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqGroupMst> getGroups(@PathVariable String lcId) {
        return this.eqGroupMstService.getGroupsByLcId(lcId);
    }

    /**
     * 센터별 설비 그룹 타입으로 필터링 조회
     */
    @GetMapping("/{lcId}/type/{eqGroupType}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqGroupMst> getGroupsByType(
            @PathVariable String lcId,
            @PathVariable String eqGroupType) {
        return this.eqGroupMstService.getGroupsByType(lcId, eqGroupType);
    }

    /**
     * 설비 그룹 단건 조회 (ID)
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqGroupMst getGroup(@PathVariable String id) {
        return this.eqGroupMstService.getGroup(id);
    }

    /**
     * 설비 그룹 단건 조회 (eqGroupId)
     */
    @GetMapping("/{lcId}/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqGroupMst getGroupByEqGroupId(
            @PathVariable String lcId,
            @PathVariable String eqGroupId) {
        return this.eqGroupMstService.getGroupByEqGroupId(lcId, eqGroupId);
    }

    /**
     * 설비 그룹 내 층 목록 조회 (TbEcs2dPage 기준)
     */
    @GetMapping("/{lcId}/group/{eqGroupId}/floors")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<Integer> getFloorsByEqGroup(
            @PathVariable String lcId,
            @PathVariable String eqGroupId) {
        return this.eqGroupMstService.getFloorsByEqGroup(lcId, eqGroupId);
    }

    /**
     * 전체 설비 그룹 목록 조회 (센터 무관)
     */
    @GetMapping("/all")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqGroupMst> getAllGroups() {
        return this.eqGroupMstService.getAllGroups();
    }
}
