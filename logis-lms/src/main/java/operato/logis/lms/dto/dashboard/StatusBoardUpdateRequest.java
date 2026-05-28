package operato.logis.lms.dto.dashboard;

import lombok.Getter;
import lombok.Setter;
import operato.logis.lms.entity.dashboard.StatusBoardDmi;

import java.util.List;

@Getter
@Setter
public class StatusBoardUpdateRequest {

    private List<StatusBoardDmi> addedList;

    private List<StatusBoardDmi> updatedList;

    private List<String> deletedIds;

    private String lcId;

    private String pageId;

    private String dimension;
}