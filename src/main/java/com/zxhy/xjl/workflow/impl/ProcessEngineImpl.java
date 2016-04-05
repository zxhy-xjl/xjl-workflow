package com.zxhy.xjl.workflow.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.activiti.engine.impl.json.JsonObjectConverter;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.rewrite.MapRewritePolicy;
import org.junit.internal.runners.model.EachTestNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.zxhy.xjl.workflow.ProcessEngine;
/**
 * 
 * @author leasonlive
 *
 */
@Service("processEngine")
public class ProcessEngineImpl implements ProcessEngine {
	private static Log log = LogFactory.getLog(ProcessEngine.class);
	@Autowired(required=true)
	RestTemplate restTemplate;
	HttpHeaders headers;
	HttpEntity<String> _noParamEntity;
	String baseURI = null;
	
	public ProcessEngineImpl() {
		this.initHeader();
		this.initURI();
	}
	/**
	 * 初始化rest的请求头
	 */
	private void initHeader(){
		this.headers = new HttpHeaders();
		this.headers.setContentType(MediaType.APPLICATION_JSON);        
        String encodedToken = "Basic "+Base64.encodeBase64String("kermit:kermit".getBytes());
        this.headers.set("Authorization",encodedToken );
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
		_noParamEntity = new HttpEntity<String>("", this.headers);
	}
	public HttpEntity<String> getHttpEntity(JSONObject json){
		if (json == null){
			return this._noParamEntity;
		} else {
			log.debug("json:" + json.toString());
			return new HttpEntity<String>(json.toString(), this.headers);
		}
	}
	
	/**
	 * 初始化工作流服务器的rest基本地址
	 */
	private void initURI(){
		this.baseURI = "http://localhost:8880/activiti-rest/service";
	}
	
	
	public String getDeployments(int start) {
       String serviceBase = this.baseURI + "/repository/deployments?start="+start;
       return this.getRestString(serviceBase, HttpMethod.GET, null);
      
       
	}
	public String getDeployment(String deployId) {
		 String serviceBase = this.baseURI + "/repository/deployments/" + deployId;
		 return this.getRestString(serviceBase, HttpMethod.GET, null);
	      
	}
	public String getProcessDefinitions(int start) {
		String serviceBase =this.baseURI + "/repository/process-definitions?start=" + start;
	    return this.getRestString(serviceBase, HttpMethod.GET, null);
	      
	}
	/**
	 * 得到一个字节
	 * @param restURI
	 * @param method
	 * @param json
	 * @return
	 */
	private byte[] getRestByte(String restURI, HttpMethod method, JSONObject json){
		return restTemplate
        .exchange(restURI, method, this.getHttpEntity(json), byte[].class).getBody();
	}
	/**
	 * 得到一个字符串
	 * @param restURI
	 * @param method
	 * @param json
	 * @return
	 */
	private String getRestString(String restURI, HttpMethod method, JSONObject json ){
		try {
			byte[] bytes = this.getRestByte(restURI, method, json);
			if (bytes == null) {
				return null;
			}
			return new String(bytes,"UTF-8");
		} catch (RestClientException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	public String getProcessDefinitionById(String processDefinitionId) {
		String serviceBase =this.baseURI + "/repository/process-definitions/" + processDefinitionId;
		return this.getRestString(serviceBase, HttpMethod.GET, null);
	      
	}
	public String getProcessDefinitionByKey(String processDefinitionKey) {
		throw new RuntimeException("还未实现");
	      
	}
	public byte[] getProcessDefinitionImage(String processDefinitionId) {
		String serviceBase = this.baseURI + "/repository/process-definitions/" + processDefinitionId;
	       String processDefinition = this.getRestString(serviceBase, HttpMethod.GET, null);
	       JSONObject json = new JSONObject(processDefinition);
			String diagramResource = json.getString("diagramResource");
			
			String diagramResource2 = this.getRestString(diagramResource, HttpMethod.GET, null);
			String contentUrl= new JSONObject(diagramResource2).getString("contentUrl");
			byte[] image = this.getRestByte(contentUrl, HttpMethod.GET, null);
			
			 return image;
			
	      
	}
	public String startProcessById(String processDefinitionId, String businessKey,JSONArray variables) {
		log.debug("start process by id:" + processDefinitionId);
		String serviceBase = this.baseURI + "/runtime/process-instances";
		JSONObject json = new JSONObject();
		json.put("processDefinitionId", processDefinitionId);
		json.put("businessKey", businessKey);
		json.put("variables", variables);
	    String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
	   return new JSONObject(resp).getString("id");
	      
	}
	public String startProcessByKey(String processDefinitionKey, String businessKey,JSONArray variables) {
		log.debug("start process by key:" + processDefinitionKey);
		String serviceBase = this.baseURI + "/runtime/process-instances";
		JSONObject json = new JSONObject();
		json.put("processDefinitionKey", processDefinitionKey);
		json.put("businessKey", businessKey);
//		json.put("tenantId", "testtenantId");
		json.put("variables", variables);
	    String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
	    log.debug("processInstance:" + resp);
	    return new JSONObject(resp).getString("id");
	}
	

	public List<TaskInfo> getMyTask(String userId) {
		String serviceBase = this.baseURI + "/runtime/tasks?assignee="+userId;
	    String resp = this.getRestString(serviceBase, HttpMethod.GET, null);
	    log.debug("getMyTask:" + resp);
		JSONObject json = new JSONObject(resp);
		JSONArray data = json.getJSONArray("data");
		log.debug("data.length:" + data.length());
		List<TaskInfo> list = new ArrayList<TaskInfo>();
		for (int i = 0; i < data.length(); i++){
			log.debug("data[" + i + "]:" + data.getJSONObject(i));
			list.add(new TaskInfo(data.getJSONObject(i)));
		}
		return list;
	}
	public List<TaskInfo> getMyTask(String userId, String processDefinitionKey) {
		log.debug("get my task userId:" + userId + " processDefinitionKey:" + processDefinitionKey);
		String serviceBase = this.baseURI + "/runtime/tasks?assignee="+userId + "&processDefinitionKey=" + processDefinitionKey;
	    String resp = this.getRestString(serviceBase, HttpMethod.GET, null);
	    log.debug("getMyTask:" + resp);
		JSONObject json = new JSONObject(resp);
		JSONArray data = json.getJSONArray("data");
		log.debug("data.length:" + data.length());
		List<TaskInfo> list = new ArrayList<TaskInfo>();
		for (int i = 0; i < data.length(); i++){
			log.debug("data[" + i + "]:" + data.getJSONObject(i));
			list.add(new TaskInfo(data.getJSONObject(i)));
		}
		return list;
	}
	public List getCandidaTask(String userId) {
		String serviceBase =this.baseURI + "/query/tasks";
		JSONObject json = new JSONObject();
		json.put("candidateUser", userId);
		String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
	    log.debug("candida:" + resp);
		JSONObject jsonTask  = new JSONObject(resp);
		return null;
	}

	public List getGroupTask(String userId) {
		String serviceBase = this.baseURI + "/identity/groups";
		JSONObject json = new JSONObject();
		json.put("member", userId);
		String resp = this.getRestString(serviceBase, HttpMethod.GET, null);
	    log.debug("groups:" + resp);
	    JSONObject jsonGroup = new JSONObject(resp);
	    JSONArray array = jsonGroup.getJSONArray("data");
	    String groups = "";
	    for (int i = 0; i < array.length(); i++){
	    	if (i > 0){
	    		groups += ",";
	    	}
	    	groups += array.getJSONObject(i).getString("id");
	    	
	    }
	    log.debug("groups:" + groups);
	    //这里只是跟任务有关的，比如admin、engineering、user三个组也会有任务，但是不在业务组中
	    this.getGroupTaskList("management,marketing,sales");
		return null;
	}
	public List getGroupTaskList(String groups) {
		String serviceBase =  this.baseURI+ "/runtime/tasks?candidateGroups="+groups;
		JSONObject json = new JSONObject();
		String resp = this.getRestString(serviceBase, HttpMethod.GET, null);
	    log.debug("groupTask:" + resp);
	    return null;
	}
	public void claimTask(String taskId, String userId) {
		String serviceBase =  this.baseURI+ "/runtime/tasks/"+taskId;
		JSONObject json = new JSONObject();
		json.put("action", "claim");
		json.put("assignee", userId);
		this.getRestString(serviceBase, HttpMethod.POST, json);
	}

	public void completeTask(String taskId,JSONArray variables) {
		log.debug("complete task:" + taskId);
		String serviceBase =  this.baseURI+ "/runtime/tasks/"+taskId;
		JSONObject json = new JSONObject();
		json.put("action", "complete");
		json.put("variables", variables);
		String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
		log.debug("resp:" + resp);
	}

	public List getHistoryTask(String userId) {
		// TODO Auto-generated method stub
		return null;
	}
	public void addUser(String id, String firstName, String lastName, String email,String password){
		
		String serviceBase =  this.baseURI+ "/identity/users";
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("firstName", firstName);
		json.put("lastName", lastName);
		json.put("email", email);
		json.put("password", password);
		String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
		log.debug("add user resp:" + resp);
	}
	public void addUserToGroup(String userId, String groupId) {
		String serviceBase =  this.baseURI+ "/identity/groups/" + groupId + "/members";
		JSONObject json = new JSONObject();
		json.put("userId", userId);
		String resp = this.getRestString(serviceBase, HttpMethod.POST, json);
		log.debug("add user to Group resp:" + resp);
	}

	
}
