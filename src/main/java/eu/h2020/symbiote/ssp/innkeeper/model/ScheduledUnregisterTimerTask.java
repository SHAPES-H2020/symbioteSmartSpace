package eu.h2020.symbiote.ssp.innkeeper.model;

import eu.h2020.symbiote.ssp.communication.rest.InnkeeperResourceStatus;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ScheduledUnregisterTimerTask extends TimerTask {

    private static Log log = LogFactory.getLog(ScheduledUnregisterTimerTask.class);

    private static ResourceRepository resourceRepository;
    private String resourceId;
    private Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap;
    private Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap;

    public ScheduledUnregisterTimerTask(ResourceRepository resourceRepository, String resourceId,
                                        Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap,
                                        Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(resourceId,"resourceId can not be null!");
        this.resourceId = resourceId;

        Assert.notNull(unregisteringTimerTaskMap,"unregisteringTimerTaskMap can not be null!");
        this.unregisteringTimerTaskMap = unregisteringTimerTaskMap;

        Assert.notNull(offlineTimerTaskMap,"offlineTimerTaskMap can not be null!");
        this.offlineTimerTaskMap = offlineTimerTaskMap;
    }

    public void run() {
        log.debug("Periodic resource unregister task for resourceId = " + resourceId +
                " STARTED :" + new Date(new Date().getTime()));

        InnkeeperResource resource = resourceRepository.findOne(resourceId);

        if (resource == null) {
            log.info("The resource with id = " + resourceId + " is not registered.");
        } else {
            log.info("The resource with id = " + resourceId + " has been unregistered.");
            resourceRepository.delete(resourceId);

            unregisteringTimerTaskMap.remove(resourceId);
            offlineTimerTaskMap.remove(resourceId);
        }

        log.debug("Periodic resource unregister task for resourceId = " + resourceId +
                " FINISHED :" + new Date(new Date().getTime()));
    }
}