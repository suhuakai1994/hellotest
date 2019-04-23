package com.huawei.oss.at.corba.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class InvokeParam {

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    public String getInterfaceTypeId() {
        return interfaceTypeId;
    }

    public void setInterfaceTypeId(String interfaceTypeId) {
        this.interfaceTypeId = interfaceTypeId;
    }

    public String getInterfaceNamingPath() {
        return interfaceNamingPath;
    }

    public void setInterfaceNamingPath(String interfaceNamingPath) {
        this.interfaceNamingPath = interfaceNamingPath;
    }

    public Map<String, Object> getInterfaceParams() {
        return interfaceParams;
    }

    public void setInterfaceParam(String paramName, Object paramValue) {
        this.interfaceParams.put(paramName, paramValue);
    }

    public void setInterfaceParamsJson(String interfaceParamsJson) {
        this.interfaceParamsJson = interfaceParamsJson;
        this.interfaceParams.clear();
        JSONObject jsonObject = JSON.parseObject(this.interfaceParamsJson);
        for (String jsonKey : jsonObject.keySet()) {
            this.interfaceParams.put(jsonKey, jsonObject.get(jsonKey));
        }
    }

    public String getIfRespositoryEnv() {
        return ifRespositoryEnv;
    }

    public void setIfRespositoryEnv(String ifRespositoryEnv) {
        this.ifRespositoryEnv = ifRespositoryEnv;
    }

    private String hostIp;

    private String userName;

    private String userPwd;

    private String interfaceTypeId;

    private String interfaceNamingPath;

    private String interfaceParamsJson;

    private String ifRespositoryEnv;

    private Map<String, Object> interfaceParams = new HashMap<>();
}
