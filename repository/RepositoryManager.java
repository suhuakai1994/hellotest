package com.huawei.oss.at.corba.repository;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Repository;
import org.omg.CORBA.RepositoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class RepositoryManager {

    public static final RepositoryManager getInstance() {
        return RepositoryManager.RepositoryManagerHolder.INSTANCE;
    }

    public Repository getInterfaceRepository(String interfacerepEnv) {
        logger.info("begin get interface repository");

        if (envRepositoryMap.containsKey(interfacerepEnv)) {
            logger.info("{} interface respository found", interfacerepEnv);
            return envRepositoryMap.get(interfacerepEnv);
        }

        locker.lock();
        try {
            if (envRepositoryMap.containsKey(interfacerepEnv)) {
                logger.info("{} intreface repository found after lock", interfacerepEnv);
                return envRepositoryMap.get(interfacerepEnv);
            }
            String ifCorbaloc = String.format("InterfaceRepository=corbaloc::1.2@%s/InterfaceRepository", interfacerepEnv);
            ORB orb = ORB.init(new String[]{"-ORBInitRef", ifCorbaloc}, null);
            org.omg.CORBA.Object irObj = orb.resolve_initial_references("InterfaceRepository");
            Repository repository = RepositoryHelper.narrow(irObj);
            if (repository != null) {
                logger.info("{} interface repository init successfully", interfacerepEnv);
                envRepositoryMap.put(interfacerepEnv, repository);
                return repository;
            }
        } catch (Exception ex) {
            logger.error("get interface repository catch exception", ex);
        } finally {
            locker.unlock();
        }

        logger.error("{} interface repository init failed", interfacerepEnv);
        throw new CorbaRequestAbnormalException("get interface repository failed");
    }

    private static class RepositoryManagerHolder {
        private static final RepositoryManager INSTANCE = new RepositoryManager();
    }

    private RepositoryManager() {
        envRepositoryMap = new ConcurrentHashMap<>();
    }

    private static Lock locker = new ReentrantLock();

    private Map<String, Repository> envRepositoryMap;

    private static final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);
}
