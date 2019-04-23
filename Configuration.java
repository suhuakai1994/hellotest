package com.huawei.oss.at.corba;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "at.execute")
public class Configuration
{

    public String getInterfacerepEnv()
    {
        return interfacerepEnv;
    }

    public void setInterfacerepEnv(String interfacerepEnv)
    {
        this.interfacerepEnv = interfacerepEnv;
    }

    private String interfacerepEnv;
}
