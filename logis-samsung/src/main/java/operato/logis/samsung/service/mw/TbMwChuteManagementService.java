package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.entity.mw.TbMwChute;
import operato.logis.samsung.entity.mw.TbMwChuteHist;
import operato.logis.samsung.query.SamsungQueryStore;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TbMwChuteManagementService extends AbstractQueryService {

    private final SamsungQueryStore samsungQueryStore;

    /**
     * Pallet Conveyor 번호 -> Chute 정보 조회
     *
     * @param endPointCd Pallet Conveyor 번호
     * @return Chute에 대한 전체 정보
     */
    public TbMwChute getChute(String endPointCd) {
        String sql = "select * from tb_mw_chutes where end_point_cd = :endPointCd";
        Map<String, Object> param = ValueUtil.newMap("endPointCd", endPointCd);
        return this.queryManager.selectBySql(sql, param, TbMwChute.class);
    }

    /**
     * 상품코드에 따른 -> Chute 정보 조회
     *
     * @param itemCode Item inner_item_code
     * @return Chute에 대한 전체 정보
     */
    public TbMwChute getChuteByItemCode(String itemCode) {
        if (ValueUtil.isEmpty(itemCode)) {
            return null;
        }

        String sql = "select * from tb_mw_chutes where item_code = :itemCode order by end_point_cd limit 1";
        Map<String, Object> param = ValueUtil.newMap("itemCode", itemCode);
        return this.queryManager.selectBySql(sql, param, TbMwChute.class);
    }

    /**
     * 상품코드에 따른 -> Chute 정보 조회
     *
     * @param itemCode Item inner_item_code
     * @return Chute에 대한 전체 정보
     */
    public List<TbMwChute> getChuteListByItemCode(String itemCode) {
        if (ValueUtil.isEmpty(itemCode)) {
            return null;
        }

        String sql = "select * from tb_mw_chutes where item_code = :itemCode";
        Map<String, Object> param = ValueUtil.newMap("itemCode", itemCode);
        return this.queryManager.selectListBySql(sql, param, TbMwChute.class, 0, 0);
    }

    /**
     * 가용 Chute 정보 조회
     * 1. itemCode is empty
     * 2. isUse is true
     * 3. 작업이 없는 Palletizer 우선
     * 4. 사용한지 가장 오래된 Chute 우선
     *
     * @return 가용 Chute에 대한 전체 정보
     */
    public TbMwChute getAvailableChute() {
        String sql = samsungQueryStore.getAvailableChute();
        return this.queryManager.selectBySql(sql, null, TbMwChute.class);
    }

    /**
     * 가용 Chute 전체 정보 조회
     * 1. itemCode is empty
     * 2. isUse is true
     *
     * @return 가용 Chute에 대한 전체 정보
     */
    public List<TbMwChute> getAvailableChuteList() {
        String sql = samsungQueryStore.getAvailableChuteList();
        return this.queryManager.selectListBySql(sql, null, TbMwChute.class, 0, 0);
    }

    /**
     * 가용 Chute에 Order 할당
     * @param endPointCd Pallet Conveyor 번호
     * @param itemCode 진행할 작업의 품번
     */
    public void assignOrder(String endPointCd, String orderId, String itemCode) {
        TbMwChute chute = getChute(endPointCd);
        chute.setOrderId(orderId);
        chute.setItemCode(itemCode);
        this.queryManager.update(chute, "orderId", "itemCode");

        // 이력 생성
        this.queryManager.insert(TbMwChuteHist.fromChute(chute));
    }

    /**
     * XYZ Cycle 완료 정보 반영
     * @param endPointCd Pallet Conveyor 번호
     * @param pickNum 적재된 Box 수
     */
    public void updateCycleResult(String endPointCd, int pickNum) {
        TbMwChute chute = getChute(endPointCd);
        chute.setBoxQty(chute.getBoxQty() + pickNum);
        this.queryManager.update(chute, "boxQty");

        // 이력 생성
        this.queryManager.insert(TbMwChuteHist.fromChute(chute));
    }

    /**
     * Order 완료 시 Chute 상태 초기화
     * @param endPointCd Pallet Conveyor 번호
     */
    public void updateOrderResult(String endPointCd) {
        TbMwChute chute = getChute(endPointCd);
        chute.setOrderId("");
        chute.setItemCode("");
        chute.setBoxQty(0);
        this.queryManager.update(chute, "orderId", "itemCode", "boxQty");

        // 이력 생성
        this.queryManager.insert(TbMwChuteHist.fromChute(chute));
    }

    /**
     * XYZ Pallet 배출 or 교체에 대한 Chute 상태 변경
     * @param endPointCd Pallet Conveyor 번호
     * @param palletSequence XYZ Pallet Sequence
     */
    public void updatePalletExchange(String endPointCd, String palletSequence) {
        TbMwChute chute = getChute(endPointCd);
        chute.setPalletSequence(palletSequence);
        chute.setBoxQty(0);
        this.queryManager.update(chute, "palletSequence", "boxQty");

        // 이력 생성
        this.queryManager.insert(TbMwChuteHist.fromChute(chute));
    }

    /**
     * 전체 Chute 정보 조회
     *
     * @return 전체 Chute에 대한 전체 정보
     */
    public List<TbMwChute> getAllChutes() {
        String sql = "select * from tb_mw_chutes order by end_point_cd asc";
        return this.queryManager.selectListBySql(sql, null, TbMwChute.class, 0, 0);
    }
}