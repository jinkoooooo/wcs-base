package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.entity.buffer.TbMwBufferStorageArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

/**
 * - 시퀀스버퍼 area 조회
 * - 단일, 다중 area 분리
 * - 상품 별 area 계산
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TbMwBufferStorageAreaService extends AbstractQueryService {

    public List<TbMwBufferStorageArea> getAvailableAreas() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addOrder("aisle_no", true);
        condition.addOrder("level_no", true);
        return queryManager.selectList(TbMwBufferStorageArea.class, condition);
    }
}