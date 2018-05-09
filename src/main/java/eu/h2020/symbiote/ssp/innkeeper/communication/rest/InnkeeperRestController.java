package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationResponse;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 8/24/2017.
 */


@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {

	private static Log log = LogFactory.getLog(InnkeeperRestController.class);


	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	InnkeeperSDEVRegistrationRequest innkeeperSDEVRegistrationRequest;

	@Autowired
	InnkeeperResourceRegistrationRequest innkeeperResourceRegistrationRequest;

	@Autowired
	Lwsp lwsp;

	@Value("${innk.lwsp.enabled:true}") 
	Boolean isLwspEnabled;

	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;

	@Autowired
	SessionsRepository sessionsRepository;
	public InnkeeperRestController() {

	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {					
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);

			try {
				String outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("JOIN MSG from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTRY:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperResourceRegistrationRequest.SspJoinResource(decoded_message);
				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));


				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{	
			// NO encryption
			return innkeeperResourceRegistrationRequest.SspJoinResource(payload);
		}


	}

	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {

			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);
			String outputMessage="";
			try {
				outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("REGISTRY from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_Hello:
			case LwspConstants.SDEV_AuthN:
				return new ResponseEntity<Object>(outputMessage,responseHeaders,HttpStatus.OK);

			case LwspConstants.REGISTRY:

				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspRegistry(lwsp.getSessionId(),decoded_message);
				String encodedResponse = lwsp.send_data(res.getBody().toString());
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{
			return innkeeperSDEVRegistrationRequest.SspRegistry(null,payload);
		}

	}



	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_UNREGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregistry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);
			try {
				String outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("UNREGISTRY from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTRY:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspDelete(decoded_message);
				//String encodedResponse = lwsp.send_data(res.getBody().toString());
				log.info("UNREGISTRY SDEV");
				return new ResponseEntity<Object>("",res.getHeaders(),res.getStatusCode());

			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// LWSP DISABLED
		} else { 
			return innkeeperSDEVRegistrationRequest.SspDelete(payload);

		}
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> keep_alive(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);

			try {
				String outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("KEEP ALIVE MSG from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTRY:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspKeepAlive(decoded_message);
				log.info(res.getBody().toString());
				String encodedResponse = lwsp.send_data(res.getBody().toString());
				//String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}


			// LWSP DISABLED
		} else { 
			return innkeeperSDEVRegistrationRequest.SspKeepAlive(payload);

		}
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_PUBLIC_RESOURCES, method = RequestMethod.GET)
	public ResponseEntity<Object> public_resources() throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		List<ResourceInfo> resourcesInfo = resourcesRepository.findAll();
		List<SspResource> sspResFilt = new ArrayList<SspResource>();
		for (ResourceInfo r : resourcesInfo) {
			if (r.getAccessPolicySpecifier().getPolicyType() == AccessPolicyType.PUBLIC) {
				SspResource sspRes = new SspResource();
				sspRes.setSspIdParent(r.getSspIdParent());
				sspRes.setSspIdResource(r.getSspIdResource());
				sspRes.setSymIdParent(r.getSymIdParent());
				sspRes.setInternalIdResource(r.getInternalIdResource());
				sspRes.setResource(r.getResource());
				sspResFilt.add(sspRes);				
			}
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		String resInfoString = objectMapper.writeValueAsString(sspResFilt);

		return new ResponseEntity<Object>(resInfoString,responseHeaders,httpStatus);
	}


	private ResponseEntity<Object>setCoreOnline(Boolean v){
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		innkeeperSDEVRegistrationRequest.setIsCoreOnline(v);
		innkeeperResourceRegistrationRequest.setIsCoreOnline(v);
		this.isCoreOnline=v;
		return new ResponseEntity<Object>("{\"result\"=\"OK\"}",responseHeaders,httpStatus);
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.SET_INNK_ONLINE, method = RequestMethod.POST)
	public ResponseEntity<Object> set_innk_online(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {

		return setCoreOnline(true);
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.SET_INNK_OFFLINE, method = RequestMethod.POST)
	public ResponseEntity<Object> set_innk_offline(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {		
		return setCoreOnline(false);

	}


}
