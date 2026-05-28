package operato.logis.lms.service.impl.dashboard;

import operato.logis.lms.LmsConstants;
import operato.logis.lms.dto.dashboard.StatusBoardUpdateRequest;
import operato.logis.lms.entity.dashboard.StatusBoardDmi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatusBoardDmiService extends AbstractQueryService {

    public List<StatusBoardDmi> selectListOnPage(String lcId, String pageId) {
        Query condition = OrmUtil.newConditionForExecution(LmsConstants.DOMAIN_ID);
        condition.addFilter("lcId", lcId);
        condition.addFilter("renderPageId", pageId);
        return this.queryManager.selectList(StatusBoardDmi.class, condition);
    }

    public Boolean deleteByPageId(String pageId) {
        String sql = "delete from status_board_dmi where render_page_id = :pageId";
        Map<String, Object> param = ValueUtil.newMap("pageId", pageId);
        this.queryManager.executeBySql(sql, param);

        return true;
    }

    @Transactional
    public Boolean updateListOnPage(StatusBoardUpdateRequest request) {
        String dimension = request.getDimension();

        // 1. 삭제 처리
        if (ValueUtil.isNotEmpty(request.getDeletedIds())) {
            for (String id : request.getDeletedIds()) {
                StatusBoardDmi deleteDmi = new StatusBoardDmi();
                deleteDmi.setId(id);
                this.queryManager.delete(deleteDmi);
            }
        }

        // 2. 추가 처리
        if (ValueUtil.isNotEmpty(request.getAddedList())) {
            for (StatusBoardDmi newDmi : request.getAddedList()) {
                setObjectDefaults(newDmi, dimension);
                this.queryManager.insert(newDmi);
            }
        }

        // 3. 수정 처리
        if (ValueUtil.isNotEmpty(request.getUpdatedList())) {
            for (StatusBoardDmi updateDmi : request.getUpdatedList()) {
                updateOnDimension(updateDmi, dimension);
            }
        }

        return true;
    }

    private void updateOnDimension(StatusBoardDmi dmi, String dimension) {
        if (LmsConstants.DIMENSION_2D.equals(dimension)) {
            this.queryManager.update(dmi, "modelCode", "isUse", "positionX2d", "positionY2d", "scaleX2d", "scaleY2d", "rotation2d", "flipHorizontal2d", "flipVertical2d");
        }
        else {
            this.queryManager.update(dmi,
                    "modelCode", "isUse",
                    "positionX3d", "positionY3d", "positionZ3d",
                    "scaleX3d", "scaleY3d", "scaleZ3d",
                    "rotationX3d", "rotationY3d", "rotationZ3d",
                    "boxPositionX3d", "boxPositionY3d", "boxPositionZ3d",
                    "boxScaleX3d", "boxScaleY3d", "boxScaleZ3d",
                    "boxRotationX3d", "boxRotationY3d", "boxRotationZ3d",
                    "boxIsUse");
        }
    }

    private void setObjectDefaults(StatusBoardDmi dmi, String dimension) {
        dmi.setBoxIsUse(false);
        if (LmsConstants.DIMENSION_2D.equals(dimension)) {
            dmi.setPositionX3d(0.0f);
            dmi.setPositionY3d(0.0f);
            dmi.setPositionZ3d(0.0f);
            dmi.setScaleX3d(0.0f);
            dmi.setScaleY3d(0.0f);
            dmi.setScaleZ3d(0.0f);
            dmi.setRotationX3d(0.0f);
            dmi.setRotationY3d(0.0f);
            dmi.setRotationZ3d(0.0f);
            dmi.setBoxPositionX3d(0.0f);
            dmi.setBoxPositionY3d(0.0f);
            dmi.setBoxPositionZ3d(0.0f);
            dmi.setBoxScaleX3d(0.0f);
            dmi.setBoxScaleY3d(0.0f);
            dmi.setBoxScaleZ3d(0.0f);
            dmi.setBoxRotationX3d(0.0f);
            dmi.setBoxRotationY3d(0.0f);
            dmi.setBoxRotationZ3d(0.0f);
        }
        else {
            dmi.setPositionX2d(0.0f);
            dmi.setPositionY2d(0.0f);
            dmi.setScaleX2d(0.0f);
            dmi.setScaleY2d(0.0f);
            dmi.setRotation2d(0.0f);
            dmi.setFlipHorizontal2d(false);
            dmi.setFlipVertical2d(false);
        }
    }
}