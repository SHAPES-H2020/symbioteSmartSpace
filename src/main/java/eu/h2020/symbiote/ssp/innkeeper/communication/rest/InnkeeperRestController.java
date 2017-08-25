package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.communication.rest.*;
import eu.h2020.symbiote.ssp.exception.InvalidMacAddressException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledResourceOfflineTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledUnregisterTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

/**
 * Created by vasgl on 8/24/2017.
 */

@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {

    private static Log log = LogFactory.getLog(InnkeeperRestController.class);

    private ResourceRepository resourceRepository;
    private Integer registrationExpiration;
    private Integer makeResourceOffine;
    private Timer timer;
    private Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap;
    private Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap;

    @Autowired
    public InnkeeperRestController(ResourceRepository resourceRepository,
                                   @Qualifier("registrationExpiration") Integer registrationExpiration,
                                   @Qualifier("makeResourceOffline") Integer makeResourceOffine,
                                   Timer timer,
                                   @Qualifier("unregisteringTimerTaskMap") Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap,
                                   @Qualifier("offlineTimerTaskMap") Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(registrationExpiration,"registrationExpiration can not be null!");
        this.registrationExpiration = registrationExpiration;

        Assert.notNull(makeResourceOffine,"makeResourceOffine can not be null!");
        this.makeResourceOffine = makeResourceOffine;

        Assert.notNull(timer,"Timer can not be null!");
        this.timer = timer;

        Assert.notNull(unregisteringTimerTaskMap,"unregisteringTimerTaskMap can not be null!");
        this.unregisteringTimerTaskMap = unregisteringTimerTaskMap;

        Assert.notNull(offlineTimerTaskMap,"offlineTimerTaskMap can not be null!");
        this.offlineTimerTaskMap = offlineTimerTaskMap;
    }

    @PostMapping(InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH)
    ResponseEntity<JoinResponse> join(@RequestBody JoinRequest joinRequest) {

        log.info("New join request was received for resource id = " + joinRequest.getId());

        if (joinRequest.getId() == null || joinRequest.getId().isEmpty()) {
            ObjectId objectId = new ObjectId();
            joinRequest.setId(objectId.toString());
        }

        // Create UnregistrationTimerTask
        ScheduledUnregisterTimerTask unregisterTimerTask = createUnregisterTimerTask(joinRequest.getId());
        ScheduledResourceOfflineTimerTask offlineTimerTask = createOfflineTimerTask(joinRequest.getId());

        InnkeeperResource newResource = resourceRepository.save(new InnkeeperResource(joinRequest,
                unregisterTimerTask, offlineTimerTask));
        log.info("newResource.getId() = " + newResource.getId());

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.OK, newResource.getId(),
                "", registrationExpiration);
        return ResponseEntity.ok(joinResponse);
    }

    @PostMapping(InnkeeperRestControllerConstants.INNKEEPER_LIST_RESOURCES_REQUEST_PATH)
    ResponseEntity<ListResourcesResponse> listResources(@RequestBody String id) {

        log.info("New list_resource request was received from symbIoTe device with id = " + id);

        ListResourcesResponse listResourcesResponse = new ListResourcesResponse();
        List<InnkeeperListResourceInfo> innkeeperListResourceInfoList = new ArrayList<>();
        List<InnkeeperResource> innkeeperResourceList = resourceRepository.findAll();

        for (InnkeeperResource resource : innkeeperResourceList) {
            InnkeeperListResourceInfo innkeeperListResourceInfo = new InnkeeperListResourceInfo();
            innkeeperListResourceInfo.setId(resource.getId());
            innkeeperListResourceInfo.setStatus(resource.getStatus());
            innkeeperListResourceInfo.setObservesProperty(resource.getObservesProperty());
            innkeeperListResourceInfoList.add(innkeeperListResourceInfo);
        }

        listResourcesResponse.setInnkeeperListResourceInfoList(innkeeperListResourceInfoList);
        return ResponseEntity.ok(listResourcesResponse);
    }

    @PostMapping(InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH)
    ResponseEntity<KeepAliveResponse> keepAlive(@RequestBody KeepAliveRequest req) {

        log.info("New keep_alive request was received from symbIoTe device with id = " + req.getId());

        InnkeeperResource innkeeperResource = resourceRepository.findOne(req.getId());

        if (innkeeperResource == null) {
            KeepAliveResponse response = new KeepAliveResponse("The request with id = " +
                    req.getId() + " was not registered.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        else {
            ScheduledResourceOfflineTimerTask offlineTimerTask = createOfflineTimerTask(innkeeperResource.getId());
            innkeeperResource.setOfflineEventTime(offlineTimerTask.scheduledExecutionTime());
            innkeeperResource.setStatus(InnkeeperResourceStatus.ONLINE);
            resourceRepository.save(innkeeperResource);

            KeepAliveResponse response = new KeepAliveResponse("The keep_alive request from resource with id = " +
                    req.getId() + " was received successfully!");
            return ResponseEntity.ok(response);
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<JoinResponse> httpMessageNotReadableExceptionHandler(HttpServletRequest req) {
        ObjectMapper mapper = new ObjectMapper();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestInString = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (req.getMethod().equals(HttpMethod.POST.toString()) &&
                    req.getPathInfo().equals(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                            InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH)) {
                JoinRequest joinRequest = mapper.readValue(requestInString, JoinRequest.class);
            }
        } catch (JsonMappingException e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            if (sw.toString().contains(InvalidMacAddressException.class.getName())) {
                JoinResponse joinResponse = new JoinResponse(JoinResponseResult.INVALID_MAC_ADDRESS_FORMAT, null, null, null);
                return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
            } else {
                JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
                return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
            return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
        return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
    }

    private void cancelUnregisterTimerTask(String resourceId) {
        ScheduledUnregisterTimerTask timerTask = unregisteringTimerTaskMap.get(resourceId);

        if (timerTask != null)
            timerTask.cancel();
    }

    private void cancelOfflineTimerTask(String resourceId) {
        ScheduledResourceOfflineTimerTask timerTask = offlineTimerTaskMap.get(resourceId);

        if (timerTask != null)
            timerTask.cancel();
    }

    private ScheduledUnregisterTimerTask createUnregisterTimerTask(String resourceId) {
        cancelUnregisterTimerTask(resourceId);

        ScheduledUnregisterTimerTask timerTask = new ScheduledUnregisterTimerTask(resourceRepository,
                resourceId, unregisteringTimerTaskMap, offlineTimerTaskMap);
        timer.schedule(timerTask, registrationExpiration);
        unregisteringTimerTaskMap.put(resourceId, timerTask);

        return timerTask;
    }

    private ScheduledResourceOfflineTimerTask createOfflineTimerTask(String resourceId) {
        cancelOfflineTimerTask(resourceId);

        ScheduledResourceOfflineTimerTask timerTask = new ScheduledResourceOfflineTimerTask(resourceRepository,
                resourceId, offlineTimerTaskMap);
        timer.schedule(timerTask, makeResourceOffine);
        offlineTimerTaskMap.put(resourceId, timerTask);

        return timerTask;
    }
}
