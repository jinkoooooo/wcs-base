package operato.logis.ecs.tspg4way.dashboard.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import operato.logis.ecs.tspg4way.dashboard.service.impl.TbEqMstService;
import operato.logis.ecs.tspg4way.entity.TbEqMst;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 설비 마스터 Controller (대시보드용)
 * 대시보드에서 eqId(= TbEqMst.id) 매핑을 위한 설비 목록 조회 API
 *
 * shuttle_layout_pagesURL은 기존과 동일하게 유지한다.
 * shuttle_layout_pages내부 조회 기준만 TbEqMst 엔티티 스펙(id, type 등)에 맞춘다.
 */
@RestController
@RequestMapping(value = "/rest/tb_eq_mst", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "설비 마스터 API")
public class TbEqMstController {

    @Autowired
    private TbEqMstService eqMstService;

    /**
     * 설비 그룹별 설비 목록 조회
     * GET /rest/tb_eq_mst/group/{eqGroupId}
     */
    @GetMapping("/group/{eqGroupId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEquipmentsByGroup(@PathVariable String eqGroupId) {
        return this.eqMstService.getEquipmentsByGroup(eqGroupId);
    }

    /**
     * 설비 그룹 + 설비 타입별 설비 목록 조회 (매핑용 콤보박스)
     * GET /rest/tb_eq_mst/group/{eqGroupId}/type/{eqType}
     *
     * - URL 파라미터 이름(eqType)은 유지
     * - 내부적으로는 TbEqMst.type 컬럼 기준으로 조회
     */
    @GetMapping("/group/{eqGroupId}/type/{eqType}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEquipmentsByGroupAndType(
            @PathVariable String eqGroupId,
            @PathVariable String eqType) {
        return this.eqMstService.getEquipmentsByGroupAndType(eqGroupId, eqType);
    }

    /**
     * 설비 타입별 전체 설비 목록 조회 (설비그룹 무관)
     * GET /rest/tb_eq_mst/type/{eqType}
     */
    @GetMapping("/type/{eqType}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEquipmentsByType(@PathVariable Integer eqType) {
        return this.eqMstService.getEquipmentsByType(eqType);
    }

    /**
     * 설비 단건 조회 (ID)
     * GET /rest/tb_eq_mst/detail/{id}
     */
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqMst getEquipment(@PathVariable String id) {
        return this.eqMstService.getEquipment(id);
    }

    /**
     * 설비 단건 조회 (eqId)
     * GET /rest/tb_eq_mst/eq/{eqId}
     *
     * tb_ecs_2d_page 현재 엔티티 기준에서는 eqId == id 로 간주하여 id로 조회
     */
    @GetMapping("/eq/{eqId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public TbEqMst getEquipmentByEqId(@PathVariable String eqId) {
        return this.eqMstService.getEquipmentById(eqId);
    }

    /**
     * 설비 그룹별 설비 타입 목록 조회 (중복 제거)
     * GET /rest/tb_eq_mst/group/{eqGroupId}/types
     */
    @GetMapping("/group/{eqGroupId}/types")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<String> getEquipmentTypesByGroup(@PathVariable String eqGroupId) {
        return this.eqMstService.getEquipmentTypesByGroup(eqGroupId);
    }

    /**
     * 전체 설비 목록 조회
     * GET /rest/tb_eq_mst/all
     */
    @GetMapping("/all")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getAllEquipments() {
        return this.eqMstService.getAllEquipments();
    }

    /**
     * PLC별 설비 목록 조회
     * GET /rest/tb_eq_mst/plc/{plcId}
     */
    @GetMapping("/plc/{plcId}")
    @Transactional(readOnly = true)
    @ResponseStatus(HttpStatus.OK)
    public List<TbEqMst> getEquipmentsByPlc(@PathVariable String plcId) {
        return this.eqMstService.getEquipmentsByPlc(plcId);
    }
}
