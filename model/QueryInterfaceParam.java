package com.huawei.oss.at.corba.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryInterfaceParam {

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getInterfaceRepositoryEnv() {
        return interfaceRepositoryEnv;
    }

    public void setInterfaceRepositoryEnv(String interfaceRepositoryEnv) {
        if (interfaceRepositoryEnv == null) {
            interfaceRepositoryEnv = "";
        }
        this.interfaceRepositoryEnv = interfaceRepositoryEnv;
    }

    public String getCorbaObjectNamingPath() {
        return corbaObjectNamingPath;
    }

    public void setCorbaObjectNamingPath(String corbaObjectNamingPath) {
        this.corbaObjectNamingPath = corbaObjectNamingPath;
    }

    private String hostIp;

    private String interfaceRepositoryEnv;

    private String corbaObjectNamingPath;

    @Override
    public String toString() {
        return String.format("%s_%s_%s", hostIp, corbaObjectNamingPath, interfaceRepositoryEnv);
    }

    private static Logger logger = LoggerFactory.getLogger(QueryInterfaceParam.class);
}
