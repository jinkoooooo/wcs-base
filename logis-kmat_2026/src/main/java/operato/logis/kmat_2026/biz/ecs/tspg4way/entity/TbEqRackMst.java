package operato.logis.kmat_2026.biz.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Table(name = "tb_eq_rack_mst", idStrategy = GenerationRule.UUID, uniqueFields = "eqId,id", indexes = {
        @Index(name = "ix_tb_eq_rack_mst_index_0", columnList = "eq_id,id", unique = true)
})
public class TbEqRackMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "eq_id", length = 50)
    private String eqId;

    @Column(name = "type")
    private int type;

    @Column(name = "row")
    private int row;

    @Column(name = "bay")
    private int bay;

    @Column(name = "level")
    private int level;

    @Column(name = "sku_id", length = 50)
    private String skuId;

    @Column(name = "sku_qty")
    private int skuQty;

    @Column(name = "status")
    private int status;

    @Column(name = "error_id", length = 50)
    private String errorId;

    @Column(name = "error_desc", length = 400)
    private String errorDesc;

    @Column(name = "use_yn")
    private boolean useYn;
    @Column(name = "cargo_yn")
    private boolean cargo_yn;


    @Column(name = "drive_only_yn")
    private boolean driveOnlyYn;


    public static List<TbEqRackMst> testDataList(){
        String eqId = "TEST_EQ_RACK_1";
        List<TbEqRackMst> testDatalist = new ArrayList<>();
        List<Integer> driveOnlyLineList = Arrays.asList(2,6);

        for(int i=1;i<=2;i++){
            for(int j=1;j<=6;j++){
                int finalJ = j;
                boolean driveOnlyYn = false;
                if(driveOnlyLineList.contains(finalJ)){
                    driveOnlyYn = true;
                }
                for(int k=1;k<=4;k++){
                    String cellId = i+"0"+j+"0"+k;
                    if(j==5 && k==1){
                        testDatalist.add(testData(eqId,cellId, EcsDBConsts.RackType.CHARGE_ENTER_PORT, driveOnlyYn));
                        continue;
                    }
                    testDatalist.add(testData(eqId,cellId, EcsDBConsts.RackType.CELL, driveOnlyYn));
                }
            }
        }
        testDatalist.add(testData(eqId,"10500", EcsDBConsts.RackType.CHARGE_PORT, false));
        testDatalist.add(testData(eqId,"20500", EcsDBConsts.RackType.CHARGE_PORT, false));

        return  testDatalist;
    }

    private static TbEqRackMst testData(String eqId, String cellId, EcsDBConsts.RackType cellType, boolean driveOnlyYn ){
        int level = Character.getNumericValue(cellId.charAt(0));
        int row = Character.getNumericValue(cellId.charAt(2));
        int bay = Character.getNumericValue(cellId.charAt(4));

        TbEqRackMst test = new TbEqRackMst();
        test.setEqId(eqId);
        test.setId(cellId);
        test.setType(cellType.getValue());
        test.setRow(row);
        test.setBay(bay);
        test.setLevel(level);
        test.setStatus(EcsDBConsts.EqRackStatus.READY.getValue());
        test.setDriveOnlyYn(driveOnlyYn);
        return test;
    }
}
