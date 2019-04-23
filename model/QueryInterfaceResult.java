package com.huawei.oss.at.corba.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryInterfaceResult {

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    @Override
    public String toString() {
        return String.format("name:%s,typeId:%s", interfaceName, interfaceType);
    }

    @JsonProperty(value = "interface_name")
    private String interfaceName;

    @JsonProperty(value = "type_id")
    private String interfaceType;
}
