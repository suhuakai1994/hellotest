package com.huawei.oss.at.corba.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.huawei.oss.at.corba.Configuration;
import com.huawei.oss.at.corba.model.InvokeParam;
import com.huawei.oss.at.corba.model.InvokeResult;
import com.huawei.oss.at.corba.orb.TokenUtil;
import com.huawei.oss.at.corba.service.InterfaceInvokeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class InterfaceInvokeController {

    @ResponseBody
    @RequestMapping(path = "/corba/**", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> invokeInterface(
            @RequestParam(name = "naming", required = true) String namingPath,
            @RequestParam(name = "repository", required = true) String repositoryId,
            @RequestBody(required = true) String param,
            @RequestHeader(required = true) Map<String, String> requestHeader) {
        logger.info("received corba invoke request");
        requestHeader.forEach((k, v) -> {
            if (k.startsWith("x-")) {
                logger.info("corba request key {} value {}", k, v);
            }
        });

        String u2020Ip = requestHeader.get("x-u2020-ip");
        String u2020Version = requestHeader.get("x-u2020-version");
        String u2020UserName = requestHeader.get("x-u2020-user");
        String u2020UserPwd = requestHeader.get("x-u2020-password");
        String u2020ItfTokenName = requestHeader.get("x-u2020-interface-token");

        InvokeParam invokeParam = new InvokeParam();
        invokeParam.setHostIp(u2020Ip);
        invokeParam.setUserName(u2020UserName);
        invokeParam.setUserPwd(u2020UserPwd);
        invokeParam.setInterfaceNamingPath(namingPath);
        invokeParam.setInterfaceTypeId(repositoryId);
        invokeParam.setInterfaceParamsJson(param);
        invokeParam.setIfRespositoryEnv(appConfig.getInterfacerepEnv());
        JSONArray token = TokenUtil.getInterfaceInvokeToken(invokeParam, u2020Version);
        invokeParam.setInterfaceParam(u2020ItfTokenName, token);

        InvokeResult invokeResult = interfaceInvokeService.invokeInterface(invokeParam);
        return new ResponseEntity<>(JSON.toJSONString(invokeResult), invokeResult.getStatus());
    }

    @ResponseBody
    @RequestMapping(path = "/executeInterface", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> excuteInterface(
            @RequestParam(value = "host", required = true) String host,
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "naming_path", required = true) String naming_path,
            @RequestParam(value = "repository_id", required = true) String repository_id,
            @RequestParam(value = "param", required = true) String param,
            @RequestParam(value = "ifRepositoryEnv", required = false) String ifRepositoryEnv) {

        InvokeParam invokeParam = new InvokeParam();
        invokeParam.setHostIp(host);
        invokeParam.setUserName("admin");
        invokeParam.setUserPwd("Aa@12345678");
        invokeParam.setInterfaceNamingPath(naming_path);
        invokeParam.setInterfaceTypeId(repository_id);
        invokeParam.setInterfaceParamsJson(param);
        invokeParam.setIfRespositoryEnv(StringUtils.isBlank(ifRepositoryEnv) ? appConfig.getInterfacerepEnv() : ifRepositoryEnv);
        JSONArray token = TokenUtil.getInterfaceInvokeToken(invokeParam, "iManagerU2020V300R019C00");
        invokeParam.setInterfaceParam("token", token);

        InvokeResult invokeResult = interfaceInvokeService.invokeInterface(invokeParam);
        return new ResponseEntity<>(JSON.toJSONString(invokeResult), invokeResult.getStatus());
    }

    @Autowired
    private InterfaceInvokeService interfaceInvokeService;

    @Autowired
    private Configuration appConfig;

    private static Logger logger = LoggerFactory.getLogger(InterfaceInvokeController.class);
}
