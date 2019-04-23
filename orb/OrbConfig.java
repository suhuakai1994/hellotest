package com.huawei.oss.at.corba.orb;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import com.huawei.oss.at.corba.model.InvokeParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrbConfig {

    public boolean isSSLMode() {
        return isSSLMode;
    }

    public void setSSLMode(boolean SSLMode) {
        isSSLMode = SSLMode;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private boolean isSSLMode;

    private String hostIp;

    private int port;

    @Override
    public String toString() {
        return String.format("OrbConfig{isSSLMode=%s,hostIp=%s,port=%d}", this.isSSLMode, this.hostIp, this.port);
    }

}
