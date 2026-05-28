package operato.logis.lms.service.impl.dashboard;

import operato.logis.lms.LmsConstants;
import operato.logis.lms.entity.dashboard.StatusBoardDmg;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;

@Service
public class StatusBoardDmgService extends AbstractQueryService {

    public StatusBoardDmg findOne(String lcId, String groupCode) {
        Query condition = OrmUtil.newConditionForExecution(LmsConstants.DOMAIN_ID);
        condition.addFilter("lcId", lcId);
        condition.addFilter("groupCode", groupCode);
        return this.queryManager.selectByCondition(StatusBoardDmg.class, condition);
    }

    public List<StatusBoardDmg> findList(String lcId) {
        Query condition = OrmUtil.newConditionForExecution(LmsConstants.DOMAIN_ID);
        condition.addFilter("lcId", lcId);
        condition.addOrder("groupType", true);
        condition.addOrder("modelType", true);
        condition.addOrder("groupCode", true);
        return this.queryManager.selectList(StatusBoardDmg.class, condition);
    }

    public boolean update2DAttributes(StatusBoardDmg attributes) {
        StatusBoardDmg dmg = findOne(attributes.getLcId(), attributes.getGroupCode());
        if (ValueUtil.isEmpty(dmg)) {
            return false;
        }

        dmg.setPositionX2d(attributes.getPositionX2d());
        dmg.setPositionY2d(attributes.getPositionY2d());
        dmg.setScaleX2d(attributes.getScaleX2d());
        dmg.setScaleY2d(attributes.getScaleY2d());
        dmg.setRotation2d(attributes.getRotation2d());
        dmg.setFlipHorizontal2d(attributes.getFlipHorizontal2d());
        dmg.setFlipVertical2d(attributes.getFlipVertical2d());

        this.queryManager.update(dmg, "positionX2d", "positionY2d", "scaleX2d", "scaleY2d", "rotation2d", "flipHorizontal2d", "flipVertical2d");

        return true;
    }
}
