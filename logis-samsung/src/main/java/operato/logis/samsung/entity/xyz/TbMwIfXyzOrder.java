package operato.logis.samsung.entity.xyz;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.mw.TbMwBcrItemDimensionAvg;
import operato.logis.samsung.entity.mw.TbMwItemMaster;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_if_xyz_order", idStrategy = GenerationRule.UUID)
public class TbMwIfXyzOrder extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "task_id", nullable = false, length = 30)
    private String taskId;

    @Column(name = "order_type", nullable = false, length = 10)
    private Integer orderType;

    @Column(name = "start_point_cd", length = 20)
    private String startPointCd;

    @Column(name = "end_point_cd", length = 20)
    private String endPointCd;

    @Column(name = "target_num", nullable = false)
    private Integer targetNum;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "length", nullable = false)
    private Integer length;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "material", length = 100)
    private String material;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "pallet_capacity", length = 10)
    private String palletCapacity;

    /***
     * 기존로직
     */
    public static TbMwIfXyzOrder setXyzOrder(TbMwXyzOrder order, TbMwItemMaster itemMaster, int boxListSize) {
        TbMwIfXyzOrder task = new TbMwIfXyzOrder();
        task.setTaskId(order.getOrderId() + "_" + String.format("%06d", order.getTaskNo() + 1));
        task.setOrderType(1);
        task.setStartPointCd(order.getStartPointCd());
        task.setEndPointCd(order.getEndPointCd());
        task.setTargetNum(boxListSize);
        task.setPriority(order.getPriority());
        // XYZ 요청사항 : Length가 Width보다 더 길어야됨
        if (itemMaster.getItemLength() > itemMaster.getItemWidth()) {
            task.setLength(itemMaster.getItemLength());
            task.setWidth(itemMaster.getItemWidth());
        } else {
            task.setLength(itemMaster.getItemWidth());
            task.setWidth(itemMaster.getItemLength());
        }
        task.setHeight(itemMaster.getItemHeight());
        task.setWeight((int) Math.ceil(itemMaster.getItemWeight()));
        task.setMaterial(itemMaster.getItemCode());
        task.setBarcode(itemMaster.getInnerItemCode());
        task.setPalletCapacity(itemMaster.getPalletCapacity());
        return task;
    }

    /***
     * 집계 평균치 우선, 없으면 마스터 치수 fallback
     */
    public static TbMwIfXyzOrder setXyzOrderByDimension(
            TbMwXyzOrder order,
            TbMwBcrItemDimensionAvg itemDimensionAvg,
            TbMwItemMaster itemMaster,
            int boxListSize
    ) {
        TbMwIfXyzOrder task = new TbMwIfXyzOrder();
        task.setTaskId(order.getOrderId() + "_" + String.format("%06d", order.getTaskNo() + 1));
        task.setOrderType(1);
        task.setStartPointCd(order.getStartPointCd());
        task.setEndPointCd(order.getEndPointCd());
        task.setTargetNum(boxListSize);
        task.setPriority(order.getPriority());

        // 치수 결정: 집계값(우선) -> 마스터 fallback
        Dimension dim = resolveDimension(itemDimensionAvg, itemMaster);

        // XYZ 요청사항 : Length가 Width보다 더 길어야됨
        if (dim.length >= dim.width) {
            task.setLength(dim.length);
            task.setWidth(dim.width);
        } else {
            task.setLength(dim.width);
            task.setWidth(dim.length);
        }
        task.setHeight(dim.height);

        task.setWeight((int) Math.ceil(itemMaster.getItemWeight()));
        task.setMaterial(itemMaster.getItemCode());
        task.setBarcode(itemMaster.getInnerItemCode());
        task.setPalletCapacity(itemMaster.getPalletCapacity());
        return task;
    }

    /**
     * 집계값이 유효하면 사용, 아니면 마스터 치수 사용
     * - 유효성 기준: null 아님 && 0보다 큼 (필요하면 최소값도 걸 수 있음)
     */
    private static Dimension resolveDimension(TbMwBcrItemDimensionAvg avg, TbMwItemMaster master) {

        if (avg != null && isValid(avg.getAvgLengthMm()) && isValid(avg.getAvgWidthMm()) && isValid(avg.getAvgHeightMm())) {
            return new Dimension(avg.getAvgLengthMm(), avg.getAvgWidthMm(), avg.getAvgHeightMm());
        }
        return new Dimension(master.getItemLength(), master.getItemWidth(), master.getItemHeight());
    }

    private static boolean isValid(Integer v) {
        return v != null && v > 0;
    }

    private static class Dimension {
        final int length;
        final int width;
        final int height;

        Dimension(int length, int width, int height) {
            this.length = length;
            this.width = width;
            this.height = height;
        }
    }
}