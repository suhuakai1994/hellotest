package com.huawei.oss.at.corba.session;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginConfig {

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginHost() {
        return loginHost;
    }

    public void setLoginHost(String loginHost) {
        this.loginHost = loginHost;
    }

    public static LoginConfig buildLoginConfig(String hostIp, String userName, String password) {
        try {
            LoginConfig config = new LoginConfig();
            String[] delims = hostIp.trim().split(":");
            if (delims.length == 2 && InetAddressValidator.getInstance().isValid(delims[0])) {
                config.setLoginHost(String.format("%s:31943", delims[0]));
                config.setUserName(userName);
                config.setPassword(password);
                return config;
            }
            logger.error("login host ip is not valid {}", hostIp);
        } catch (Exception ex) {
            logger.error("build login config catch exception", ex);
        }

        throw new CorbaRequestAbnormalException("build  login config error");
    }

    private String loginHost;

    private String userName;

    private String password;

    private static Logger logger = LoggerFactory.getLogger(LoginConfig.class);
}
