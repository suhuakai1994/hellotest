package com.huawei.oss.at.corba.orb;

import com.huawei.oss.at.corba.exception.CorbaRequestAbnormalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public final class OrbManager
{

    public static OrbManager getInstance()
    {
        return OrbManagerHolder.INSTANCE;
    }

    public OrbWrapper getOrbWrapper(OrbConfig orbConfig, boolean isClientMode)
    {
        String orbWrapperKey = String.format("%s_%s", orbConfig.toString(), isClientMode);

        if (this.envOrbMap.containsKey(orbWrapperKey))
        {
            logger.info(orbWrapperKey + " found");
            return this.envOrbMap.get(orbWrapperKey);
        }

        locker.lock();
        try
        {
            if (envOrbMap.containsKey(orbWrapperKey))
            {
                logger.info(orbWrapperKey + " found after lock");
                return envOrbMap.get(orbWrapperKey);
            }
            OrbWrapper orbWrapper = new OrbWrapper(orbConfig, isClientMode);
            orbWrapper.initOrb();
            logger.info(orbWrapperKey + " init successfully");
            envOrbMap.put(orbWrapperKey, orbWrapper);
            return orbWrapper;
        }
        catch (Exception ex)
        {
            logger.error("get orb wrapper catch exception", ex);
        }
        finally
        {
            locker.unlock();
        }

        logger.error(orbWrapperKey + " init failed");
        throw new CorbaRequestAbnormalException("get orb wrapper failed");
    }

    @PreDestroy
    public void clear()
    {
        for (OrbWrapper orbWrapper : envOrbMap.values())
        {
            orbWrapper.close();
        }
    }

    private static class OrbManagerHolder
    {
        private static final OrbManager INSTANCE = new OrbManager();
    }

    private OrbManager()
    {
        envOrbMap = new ConcurrentHashMap<>();
    }

    private static Lock locker = new ReentrantLock();

    private Map<String, OrbWrapper> envOrbMap;

    private static Logger logger = LoggerFactory.getLogger(OrbManager.class);
}
