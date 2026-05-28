package operato.logis.ecs.tspg4way.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "tb_eq_plc_mst", idStrategy = GenerationRule.UUID)
public class TbEqPlcMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "ip", length = 45) // IPv4/IPv6 둘 다 커버
    private String ip;

    @Column(name = "port")
    private int port;

    @Column(name = "plc_if_type", length = 30) // 예: MELSEC, S7, MODBUS...
    private String plcIfType;

    @Column(name = "plc_eq_type")
    private int plcEqType;

    @Column(name = "connect_yn")
    private boolean connectYn;

    @Column(name = "use_yn")
    private boolean useYn;


    public static List<TbEqPlcMst> testDataList(){
        List<TbEqPlcMst> testDatalist = new ArrayList<>();
        testDatalist.add(testData("TEST_EQ_CAR_1", "192.168.0.1", 5000, MelsecConsts.InterfaceType.BINARY, EcsDBConsts.PlcEqType.SHUTTLE_CAR));
        testDatalist.add(testData("TEST_EQ_CAR_2", "192.168.0.2", 5000, MelsecConsts.InterfaceType.BINARY, EcsDBConsts.PlcEqType.SHUTTLE_CAR));
        testDatalist.add(testData("TEST_EQ_CV_1", "192.168.0.3", 3000, MelsecConsts.InterfaceType.BINARY, EcsDBConsts.PlcEqType.CONVEYOR_AND_LIFT));
        return  testDatalist;
    }

    private static TbEqPlcMst testData(String eqId, String ip, int port, MelsecConsts.InterfaceType plcIfType, EcsDBConsts.PlcEqType plcEqType){
        TbEqPlcMst testData = new TbEqPlcMst();
        testData.setId(eqId);
        testData.setIp(ip);
        testData.setPort(port);
        testData.setPlcIfType(plcIfType.getValue());
        testData.setPlcEqType(plcEqType.getValue());
        return testData;
    }
}
