package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.Set;

@Service
public class WcsOrderValidationService {

    private static final Set<String> ITEM_OPTIONAL_ORDER_TYPES = Set.of(
            OrderTypeEnumCode.MOVE.codeAsString()
    );

    public String validate(WcsOrderCommand command) {
        if (command == null) return "command is required";
        if (!StringUtils.hasText(command.getOrderType())) return "orderType is required";
        if (ValueUtil.isEmpty(command.getEqGroupId())) return "eqGroupId is required";

        if (ValueUtil.isEmpty(command.getItems())) {
            if (isItemValidationSkipped(command.getOrderType())) return null;
            return "items is required and must not be empty";
        }

        for (int i = 0; i < command.getItems().size(); i++) {
            WcsOrderCommandItem item = command.getItems().get(i);
            if (item == null) return "items[" + i + "] is null";
            if (item.getLineNo() == null || item.getLineNo() <= 0) return "items[" + i + "].lineNo is required and must be positive";
            if (!StringUtils.hasText(item.getSkuCode())) return "items[" + i + "].skuCode is required";
            if (item.getQty() == null || item.getQty() <= 0) return "items[" + i + "].qty is required and must be positive";
        }
        return null;
    }

    private boolean isItemValidationSkipped(String orderType) {
        return StringUtils.hasText(orderType) && ITEM_OPTIONAL_ORDER_TYPES.contains(orderType);
    }
}