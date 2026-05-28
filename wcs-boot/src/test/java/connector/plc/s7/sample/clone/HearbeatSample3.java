/*
 * PROJECT Mokka7 (fork of Snap7/Moka7)
 * 
 * Copyright (c) 2017 J.Zimmermann (comtel2000)
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Mokka7 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE whatever license you
 * decide to adopt.
 * 
 * Contributors: J.Zimmermann - Mokka7 fork
 * 
 */
package connector.plc.s7.sample.clone;

import connector.plc.s7.sample.ClientRunner;
import operato.logis.connector.plc.s7.S7Client;
import operato.logis.connector.plc.s7.type.AreaType;
import operato.logis.connector.plc.s7.type.DataType;
import operato.logis.connector.plc.s7.util.S7;

/**
 * Clone bit of DB200.DBX34.0 to DB200.DBX34.1
 *
 * @author comtel
 *
 */
public class HearbeatSample3 extends ClientRunner {

    public HearbeatSample3() {
        super();
    }

    @Override
    public void call(S7Client client) throws Exception {
        boolean plcBit, clientBit;
        for (int i = 0; i < 1000; i++) {
            client.readArea(AreaType.DB, 200, 34, 1, DataType.BYTE, buffer);
            plcBit = S7.getBitAt(buffer, 0, 0);
            clientBit = S7.getBitAt(buffer, 0, 1);
            if (plcBit != clientBit) {
                System.err.println("update: " + plcBit + "/" + clientBit);
                S7.setBitAt(buffer, 0, 1, plcBit);
                client.writeArea(AreaType.DB, 200, 34, 1, DataType.BYTE, buffer);
            }
            Thread.sleep(500);
        }

        // client.WriteArea(S7.DB, 200, 34, 1, new
        // byte[]{0x00});
    }

    public static void main(String[] args) {
        new HearbeatSample3();
    }
}
