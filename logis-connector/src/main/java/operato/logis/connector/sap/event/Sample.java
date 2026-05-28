package operato.logis.connector.sap.event;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataProvider;

public class Sample {
/*
	telnet vhcwlcoqcs.sap.coway.do 3601

	curl -v telnet://vhcwlcoqcs.sap.coway.do:3601
	*/
	static String ABAP_AS = "ABAP_AS_WITHOUT_POOL";
    static String host = "vhcwlcoqcs.sap.coway.do";
    static String sysnr = "00";
    static String lang = "KO";
    static String client = "100";
    static String client_other = "500";
    static String group = "TITAN";
    static String r3name = "COQ";
    static String user = "CORFCWES01";	//RFC ID
    static String passwd = "Coway123!";	//RFC 비번
    
    static {
    	Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
        connectProperties.setProperty(DestinationDataProvider.JCO_USER, user);
        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, passwd);
        connectProperties.setProperty(DestinationDataProvider.JCO_LANG, lang);        
        connectProperties.setProperty(DestinationDataProvider.JCO_MSHOST, host);
        connectProperties.setProperty(DestinationDataProvider.JCO_GROUP, group);
        connectProperties.setProperty(DestinationDataProvider.JCO_R3NAME, r3name);
        createDestinationDataFile(ABAP_AS, connectProperties);
    }

    static void createDestinationDataFile(String destinationName, Properties connectProperties) {
        File destCfg = new File(destinationName + ".jcoDestination");
        try {
            FileOutputStream fos = new FileOutputStream(destCfg, false);
            connectProperties.store(fos, "for tests only !");
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create thedestination files", e);
        }
    }
 
    //normal
    public static void ZSD_1000_DSAPP_ORDER_LIST() {
    	JCoDestination destination = null;
    	try {
    		destination = JCoDestinationManager.getDestination(ABAP_AS);
	    	JCoFunction function = destination.getRepository().getFunction("ZSD_1000_DSAPP_ORDER_LIST");
	    	JCoParameterList listParam = function.getImportParameterList();
	
			listParam.setValue("I_REGER_NO", "20466089"); 
			listParam.setValue("I_YYYY_MM", "202501");
			function.execute(destination);
			
			JCoTable codes = function.getTableParameterList().getTable("ET_ORDER");
			for (int i = 0; i < codes.getNumRows(); i++) {
				codes.setRow(i);
				System.out.println("ORDER_NO : " + codes.getString("ORDER_NO"));
				
			}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    //stateful
    public static void ZSD_1000_DSAPP_ORDER_LIST2() throws JCoException {
    	JCoDestination destination = null;
    	try {
    		destination = JCoDestinationManager.getDestination(ABAP_AS);
    		JCoContext.begin(destination);
	    	JCoFunction function = destination.getRepository().getFunction("ZSD_1000_DSAPP_ORDER_LIST");
	    	JCoParameterList listParam = function.getImportParameterList();
	
			listParam.setValue("I_REGER_NO", "20466089"); 
			listParam.setValue("I_YYYY_MM", "202501");
			function.execute(destination);
			
			JCoTable codes = function.getTableParameterList().getTable("ET_ORDER");
			for (int i = 0; i < codes.getNumRows(); i++) {
				codes.setRow(i);
				System.out.println("ORDER_NO : " + codes.getString("ORDER_NO"));
				
			}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}finally {
    		JCoContext.end(destination);
		}
    }
 
    public static void main(String[] args) throws JCoException {
    	ZSD_1000_DSAPP_ORDER_LIST();
    	ZSD_1000_DSAPP_ORDER_LIST2();
    }
}
