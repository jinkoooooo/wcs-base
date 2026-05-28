package operato.logis.wcs.rest.permission;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sec.rest.PermissionController;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * tspg_4way / kmat_2026 화면이 사용하는 menuName 기반 권한 조회.
 * 공유 PermissionController 미수정 — 다른 프로젝트 영향 0.
 */
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RequestMapping("/rest/wcs/permissions")
public class TspgPermissionController {

    private static final String ALL_KEY = "all";
    private static final String MENU_ID_BY_NAME_SQL =
            "SELECT id FROM menus WHERE domain_id = :domainId AND name = :name";

    private final IQueryManager queryManager;

    /**
     * GET /rest/wcs/permissions/menu/{menuName}
     * 관리자: ["all"] / 일반: ["show","create","update","delete"] 부분 집합 / 메뉴 없음 또는 권한 없음: [].
     */
    @GetMapping(value = "/menu/{menuName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find current user's permission actions for a menu by name")
    public List<String> findActionsByMenuName(@PathVariable("menuName") String menuName) {
        if (User.isCurrentUserAdmin()) {
            return ValueUtil.newStringList(ALL_KEY);
        }
        String menuId = this.queryManager.selectBySql(
                MENU_ID_BY_NAME_SQL,
                ValueUtil.newMap("domainId,name", Domain.currentDomain().getId(), menuName),
                String.class);
        if (ValueUtil.isEmpty(menuId)) {
            return ValueUtil.newStringList();
        }
        return BeanUtil.get(PermissionController.class)
                .findMenuPermissionsByUser(User.currentUser().getId(), menuId);
    }
}
