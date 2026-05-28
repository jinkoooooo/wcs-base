package operato.logis.wcs.rest.nia;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.dto.NiaApprovalRequest;
import operato.logis.wcs.service.impl.qctest.NiaApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * 국검(NIA) 처리 API.
 *
 * dashboard2d 에서 NIA_PENDING stock 셀을 선택 후 2D 바코드 스캔.
 * 승인번호 존재 = 국검 승인됨 (별도 접수번호 개념 없음).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/inventory/nia", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "국검(NIA) 처리 API")
public class NiaController {

    private final NiaApprovalService niaApprovalService;

    /**
     * 국검 승인번호 처리 (2D 스캔).
     *
     * 흐름:
     *   NIA_PENDING            → nia_approval_no 저장 + stock_type=NORMAL 복귀
     *   NORMAL + 같은 승인번호  → 200 no-op (멱등)
     *   NORMAL + 다른 승인번호  → 409
     *   그 외 stock_type       → 409
     */
    @PostMapping(value = "/stocks/{stockId}/approval", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> applyApproval(
            @PathVariable("stockId") String stockId,
            @RequestParam("eq_group_id") String eqGroupId,
            @RequestBody NiaApprovalRequest req) {

        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eq_group_id 는 필수입니다");
        }
        if (ValueUtil.isEmpty(stockId)) {
            throw new ElidomRuntimeException("stockId 는 필수입니다");
        }
        if (ValueUtil.isEmpty(req.getNiaApprovalNo()) || req.getNiaApprovalNo().trim().isEmpty()) {
            throw new ElidomRuntimeException("nia_approval_no 는 필수입니다");
        }
        CommentValidator.requireValid("comment", req.getComment());

        return niaApprovalService.applyApproval(eqGroupId, stockId, req);
    }

    /**
     * 국검 불승인 처리 (반려).
     *
     * 흐름:
     *   NIA_PENDING → stock_type=NIA_FAIL (HOLD), 사유 audit 기록
     *   NIA_FAIL    → 200 no-op (멱등)
     *   그 외       → 409
     */
    @PostMapping(value = "/stocks/{stockId}/rejection", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> applyRejection(
            @PathVariable("stockId") String stockId,
            @RequestParam("eq_group_id") String eqGroupId,
            @RequestBody NiaApprovalRequest req) {

        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        if (ValueUtil.isEmpty(eqGroupId)) {
            throw new ElidomRuntimeException("eq_group_id 는 필수입니다");
        }
        if (ValueUtil.isEmpty(stockId)) {
            throw new ElidomRuntimeException("stockId 는 필수입니다");
        }
        CommentValidator.requireValid("comment", req.getComment());

        return niaApprovalService.applyRejection(eqGroupId, stockId, req);
    }
}
