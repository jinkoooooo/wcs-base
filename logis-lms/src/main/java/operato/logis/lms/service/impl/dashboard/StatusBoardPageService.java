package operato.logis.lms.service.impl.dashboard;

import operato.logis.lms.entity.dashboard.StatusBoardPage;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class StatusBoardPageService extends AbstractQueryService {

    public List<StatusBoardPage> findListByLcId(String lcId) {
        String sql = "select * from status_board_page where lc_id = :lcId order by page_index asc";
        Map<String, Object> param = ValueUtil.newMap("lcId", lcId);
        return this.queryManager.selectListBySql(sql, param, StatusBoardPage.class, 0, 0);
    }

    public Boolean updateName(StatusBoardPage page) {
        this.queryManager.update(page, "pageName");

        return true;
    }

    public Boolean updateIndex(List<StatusBoardPage> pageList) {
        for (StatusBoardPage page : pageList) {
            this.queryManager.update(page, "pageIndex");
        }

        return true;
    }

    public Boolean updateValue(StatusBoardPage page) {
        this.queryManager.update(page, "backgroundColor", "lengthX", "lengthY");

        return true;
    }

    public Boolean createPage(StatusBoardPage page) {
        this.queryManager.insert(page);

        return true;
    }

    public Boolean deletePage(StatusBoardPage page) {
        this.queryManager.delete(page);

        return true;
    }
}