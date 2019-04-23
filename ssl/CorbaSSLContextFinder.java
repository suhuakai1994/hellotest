package com.huawei.oss.at.corba.ssl;

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.TrustManager;
import com.swimap.external.sf.ssl.SSLManager;
import org.openorb.orb.config.ORBLoader;
import org.openorb.orb.ssl.SSLContextFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class CorbaSSLContextFinder extends SSLContextFinder {

    @Override
    public void initialize(ORBLoader loader) {
        try {
            SSLManager manager = SSLManager.defaultClientManager();
            //manager.removeProtocol(Constants.SSLv3);
            SSLSocketFactory sslSockF = manager.getSSLSocketFactory();
            SSLServerSocketFactory sslServerSockF = manager.getSSLServerSocketFactory();
            super.setFactories(sslServerSockF, sslSockF);
        } catch (Exception ex) {
            logger.error("CorbaSSLContextFinder initialize catch exception", ex);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected KeyManager[] getKeyManagers(ORBLoader orbLoader) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected TrustManager[] getTrustManagers(ORBLoader orbLoader) {
        throw new UnsupportedOperationException();
    }

    private static Logger logger = LoggerFactory.getLogger(CorbaSSLContextFinder.class);
}
