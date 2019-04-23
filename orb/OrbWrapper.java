package com.huawei.oss.at.corba.orb;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import com.huawei.oss.at.corba.repository.RepositoryManager;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Object;
import org.omg.CORBA.Repository;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAnyFactoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Properties;

public class OrbWrapper implements Closeable {

    public OrbWrapper(OrbConfig config, boolean isClientMode) {
        this.hostIp = config.getHostIp();
        this.port = config.getPort();
        this.isClientMode = isClientMode;
        this.isSSL = config.isSSLMode();
    }

    public void initOrb() {
        try {
            logger.info("begin init invoke corba orb");
            Properties props = OrbUtil.getORBInitProperties(isClientMode, isSSL);
            String[] orbArgs = OrbUtil.getORBInitArguments(isClientMode, isSSL);
            this.orb = ORB.init(orbArgs, props);
            Object naming = OrbUtil.resolveInitialNameSerivce(this.orb, this.hostIp, port, isSSL, "");
            if (naming != null) {
                ((org.openorb.orb.core.ORB) this.orb).addInitialReference("NameService", naming);
                logger.info("invoke corba orb init successfully");
                return;
            }
        } catch (Exception e) {
            logger.error("init orb catch exception", e);
        }
        throw new CorbaRequestAbnormalException("init orb failed");
    }

    public ORB getOrb() {
        return orb;
    }

    public NamingContextExt getNamingContext() {
        try {
            Object ncObj = this.orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(ncObj);
            return ncRef;
        } catch (InvalidName invalidName) {
            logger.error("get naming context catch exception", invalidName);
            throw new CorbaRequestAbnormalException("get naming context failed");
        }
    }

    public DynAnyFactory getDynAnyFactory() {
        try {
            org.omg.CORBA.Object dynAnyFactoryObj = this.orb.resolve_initial_references("DynAnyFactory");
            return DynAnyFactoryHelper.narrow(dynAnyFactoryObj);
        } catch (Exception ex) {
            logger.error("get dynanyfactory catch exception", ex);
            throw new CorbaRequestAbnormalException("get dynanyfactory failed");
        }
    }

    public Repository getInterfaceRepository(String interfacerepEnv) {
        return RepositoryManager.getInstance().getInterfaceRepository(interfacerepEnv);
    }

    public void close() {
        try {
            logger.info("begin shutdown {} orb", this.hostIp);
            if (orb != null) {
                orb.shutdown(true);
                orb.destroy();
                logger.info("shutdown {} orb successfully", this.hostIp);
            }
        } catch (Exception ex) {
            logger.error("shutdown orb catch exception", ex);
        }
    }

    private ORB orb = null;

    private String hostIp = null;

    private int port;

    private boolean isClientMode;

    private boolean isSSL;

    private static Logger logger = LoggerFactory.getLogger(OrbWrapper.class);
}
