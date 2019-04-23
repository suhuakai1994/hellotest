package com.huawei.oss.at.corba.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryParamResult {

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public int getParamKind() {
        return paramKind;
    }

    public void setParamKind(int paramKind) {
        this.paramKind = paramKind;
    }

    public String toString() {
        return String.format("paramName:%s,paramType:%s,paramValue:%s", paramName, paramType, paramValue);
    }

    @JsonProperty(value = "param_name")
    private String paramName;

    @JsonProperty(value = "param_type")
    private String paramType;

    @JsonProperty(value = "param_value")
    private String paramValue;

    @JsonProperty(value = "param_kind")
    private int paramKind;
}
