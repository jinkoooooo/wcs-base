package xyz.elidom.base.rest;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.base.service.ProvisionService;
import xyz.elidom.base.service.ProvisionServiceContants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.FormatUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/provision")
@ServiceDesc(description="Domain Provision Service API")
public class ProvisionController extends AbstractRestService {


    @Override
    protected Class<?> entityClass() {
        return Domain.class;
    }

    private Logger logger = LoggerFactory.getLogger(ProvisionController.class);

    @Autowired
    private ProvisionService provisionService;

    /**
     * WAS 가 클러스터 구성일때 캐쉬 리셋 메시지를 수신할 부분
     * 설정 : xyz.elings.redis.was.servers
     * permit Url
     * @param
     * @return
     */
    @PostMapping(value = "/createDomain", produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createDomain() {
        Long newDomainId = this.provisionService.createDomain();
        return newDomainId;
    }

    /**
     *
     * portal  provisioning I/F
     *
     * @param ifData
     * @return
     */
    @PostMapping(value = "/provisioning/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "WES Provisioning")
    public Object provisionProvider(@RequestBody Map<String, Object> ifData) throws UnsupportedEncodingException {
        //TODO : delete logger
        logger.info(FormatUtil.toJsonString(ifData, false));
        return this.provisionService.provisioning(ifData, ProvisionServiceContants.CREATE);
    }

    /**
     *
     * portal  provisioning I/F
     *
     * @param ifData
     * @return
     */
    @PostMapping(value = "/provisioning/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "WES Provisioning")
    public Object provisionUpdater(@RequestBody Map<String, Object> ifData) throws UnsupportedEncodingException {
        //TODO : delete logger
        logger.info(FormatUtil.toJsonString(ifData, false));
        return this.provisionService.provisioning(ifData, ProvisionServiceContants.UPDATE);
    }
}
