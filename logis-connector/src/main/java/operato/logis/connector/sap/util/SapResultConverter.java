package operato.logis.connector.sap.util;

import java.util.Map;

import operato.logis.connector.sap.mapper.SapMappingMetaData;
import xyz.elidom.sys.model.BaseResponse;

public final class SapResultConverter {
 private SapResultConverter(){}
 public static BaseResponse toBase(SapMappingMetaData cfg){
     Map<String,String> exp = cfg.getExportParams();
     boolean ok = "S".equalsIgnoreCase(exp.getOrDefault("O_MSGTY","E"));
     return new BaseResponse(ok,
                             exp.getOrDefault("O_MSGLIN",""),
                             cfg.getTableDataMap());   // 필요 없다면 null
 }
}