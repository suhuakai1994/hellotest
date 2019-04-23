package com.huawei.oss.at.corba.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.http.HttpStatus;

public class InvokeResult {

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public JSONArray getOutParams() {
        return outParams;
    }

    public void setOutParams(JSONArray outParams) {
        this.outParams = outParams;
    }

    public JSONObject getResult() {
        return result;
    }

    public void setResult(JSONObject result) {
        this.result = result;
    }

    public JSONObject toJSONObject() {
        return JSON.parseObject(JSON.toJSONString(this));
    }

    @JSONField(name = "status")
    private HttpStatus status;

    @JSONField(name = "params")
    private JSONArray outParams;

    @JSONField(name = "return")
    private JSONObject result;
}
