package connector.plc.melsec;

import operato.logis.connector.plc.melsec.Melsec;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecQ3E;
import org.junit.jupiter.api.Test;

public class MelsecTest {
    @Test
    void 멜섹_16진수_문자열_출력_테스트() {
        Melsec melsecAscii = new Melsec(new MelsecQ3E(MelsecConsts.InterfaceType.ASCII));
        Melsec melsecBinary = new Melsec(new MelsecQ3E(MelsecConsts.InterfaceType.BINARY));

        // System.out.println(melsecAscii.ReadBit(MelsecConsts.DeviceCode.M.getValue(), 100, 2));
        // System.out.println(melsecBinary.ReadBit(MelsecConsts.DeviceCode.M.getValue(), 100, 2));
        // System.out.println(melsecAscii.ReadWord(MelsecConsts.DeviceCode.W.getValue(), 100, 32));
        // System.out.println(melsecBinary.ReadWord(MelsecConsts.DeviceCode.TN.getValue(), 100, 3));
        System.out.println(melsecBinary.ReadWord(MelsecConsts.DeviceCode.W.getBinaryValue(), 200 , 32));
        // System.out.println(melsecAscii.WriteBit(MelsecConsts.DeviceCode.M.getValue(), 100, new int[] {1,1,1,0,0,0,1,0, 1,1,0,0,0,1,0,0 , 0,1,1,0,1,0,0,1, 1,1,0,1,0,1,0,1, 1}));
        // System.out.println(melsecBinary.WriteBit(MelsecConsts.DeviceCode.M.getValue(), 100, new int[]{1,1,1,0,0,0,1,0, 1,1,0,0,0,1,0,0 , 0,1,1,0,1,0,0,1, 1,1,0,1,0,1,0,1, 1,1,1,1}));
        // System.out.println(melsecAscii.WriteWord(MelsecConsts.DeviceCode.D.getValue(), 100, new int[] {6549,4610,4400}));
        // System.out.println(melsecBinary.WriteWord(MelsecConsts.DeviceCode.D.getValue(), 100, new int[] {6549,4610,4400}));
    }
}
