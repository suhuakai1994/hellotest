package com.huawei.oss.at.corba.controller;

import com.huawei.oss.at.corba.Configuration;
import com.huawei.oss.at.corba.model.QueryInterfaceParam;
import com.huawei.oss.at.corba.model.QueryInterfaceResult;
import com.huawei.oss.at.corba.model.QueryParamResult;
import com.huawei.oss.at.corba.service.InterfaceQueryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InterfaceQueryController {

    @ResponseBody
    @RequestMapping(path = "/queryInterface", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<QueryInterfaceResult> queryCorbaInterfaces(
            @RequestParam(value = "host", required = true) String host,
            @RequestParam(value = "naming_path", required = true) String naming_path,
            @RequestParam(value = "ifRepositoryEnv", required = false) String ifRepositoryEnv) {
        logger.info("received corba query interface request");
        QueryInterfaceParam queryInterfaceParam = new QueryInterfaceParam();
        ifRepositoryEnv = StringUtils.isBlank(ifRepositoryEnv) ? appConfig.getInterfacerepEnv() : ifRepositoryEnv;
        queryInterfaceParam.setHostIp(host);
        queryInterfaceParam.setCorbaObjectNamingPath(naming_path);
        queryInterfaceParam.setInterfaceRepositoryEnv(ifRepositoryEnv);

        return interfaceQueryService.queryInterfaces(queryInterfaceParam);
    }

    @ResponseBody
    @RequestMapping(path = "/queryParam", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<QueryParamResult> queryCorbaParams(
            @RequestParam(value = "host", required = true) String host,
            @RequestParam(value = "naming_path", required = true) String naming_path,
            @RequestParam(value = "repository_id", required = true) String repository_id,
            @RequestParam(value = "ifRepositoryEnv", required = false) String ifRepositoryEnv) {
        logger.info("received corba query interface params request");
        ifRepositoryEnv = StringUtils.isBlank(ifRepositoryEnv) ? appConfig.getInterfacerepEnv() : ifRepositoryEnv;
        return interfaceQueryService.queryInterfaceParams(repository_id, ifRepositoryEnv);
    }

    @ResponseBody
    @RequestMapping(path = "/queryObject", method = {RequestMethod.GET, RequestMethod.POST})
    public List<String> queryCorbaObjects(
            @RequestParam(value = "host", required = true) String host,
            @RequestParam(value = "naming_path", required = false) String naming_path,
            @RequestParam(value = "ifRepositoryEnv", required = false) String ifRepositoryEnv) {
        logger.info("received corba query object request");
        return interfaceQueryService.queryObjects(host, naming_path);
    }

    @Autowired
    private InterfaceQueryService interfaceQueryService;

    @Autowired
    private Configuration appConfig;

    private static final Logger logger = LoggerFactory.getLogger(InterfaceQueryController.class);
}
