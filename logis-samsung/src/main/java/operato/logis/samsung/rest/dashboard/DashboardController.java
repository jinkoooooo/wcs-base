package operato.logis.samsung.rest.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.dashboard.DashboardDate;
import operato.logis.samsung.dto.dashboard.DashboardMain;
import operato.logis.samsung.service.dashboard.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Date;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/dashboard")
@ServiceDesc(description="Dashboard Service API")
public class DashboardController {

    private final DashboardService dashboardService;

    @RequestMapping(
            value="/main",
            method= RequestMethod.GET,
            produces= MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description="날짜 정보 없는 Dashboard 정보 조회 API")
    public DashboardMain getDashboardMain(
            @RequestParam(value = "chooseDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date chooseDate
    ) {
        // 👇 파라미터 없거나 빈값이면 오늘 날짜로
        if (chooseDate == null) {
            chooseDate = new Date();
        }

        dashboardService.setDashboardMain(chooseDate);
        return DashboardService.getDashboardMain();
    }

    @RequestMapping(
            value="/date",
            method= RequestMethod.GET,
            produces= MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description="날짜 정보 있는 Dashboard 정보 조회 API")
    public DashboardDate getDashboardDate(
            @RequestParam(value = "chooseDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date chooseDate,
            @RequestParam(defaultValue = "1") int inputData
    ) {
        // 👇 여기서도 동일하게 처리
        if (chooseDate == null) {
            chooseDate = new Date();
        }

        return dashboardService.getDashboardDate(chooseDate, inputData);
    }
}
