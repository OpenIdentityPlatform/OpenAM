/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018-2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.authentication.modules.recaptcha;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import ru.org.openam.httpdump.Dump;

public class ReCaptcha extends AMLoginModule {
	
    private static final String amAuthReCaptcha = "amAuthReCaptcha";
    private static Debug debug = Debug.getInstance(amAuthReCaptcha);
    

	static Field AMIdentity_isSharedStateField;
	static{
		try{
			AMIdentity_isSharedStateField=AMLoginModule.class.getDeclaredField("isSharedState");
			AMIdentity_isSharedStateField.setAccessible(true);
		}catch(Exception e){
			debug.error("AMIdentity_isSharedStateField",e);
		}
	}
	
	static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	static {
		cm.setDefaultMaxPerRoute(500);
		cm.setMaxTotal(500);
	}
	
	final static Integer connectTimeout = Integer.parseInt(SystemProperties.get(ReCaptcha.class.getName()+".connect.timeout","1500"));
	final static Integer readTimeout = Integer.parseInt(SystemProperties.get(ReCaptcha.class.getName()+".read.timeout","2500"));
	
	final static RequestConfig requestConfig = RequestConfig.custom()
		    .setSocketTimeout(connectTimeout)
		    .setConnectTimeout(connectTimeout)
		    .setConnectionRequestTimeout(readTimeout)
		    .build();
	
	static CloseableHttpClient httpClient = HttpClients.custom()
	        .setConnectionManager(cm).setDefaultRequestConfig(requestConfig)
	        .build();
	
	public ReCaptcha() {
		super();
	}

	@SuppressWarnings("rawtypes")
	public Map sharedState;
	
	private String secret = "";
	
	private String key = "";
	
	private String jsUrl = "";
	
	private String verifyUrl = "";
	
	private boolean invisible = true;

	boolean isIPIgnore=false;
	
	@Override
	@SuppressWarnings("rawtypes") 
	public void init(Subject subject, Map sharedState, Map options) {
		if (getHttpServletRequest()==null)
			return;
		
		this.sharedState = sharedState;
		try {//iplanet-am-auth-shared-state-enabled=true
			AMIdentity_isSharedStateField.set(this, true);
		} catch (Exception e) {
			debug.error("AMIdentity_isSharedStateField",e);
		}
		
		secret = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.secret", "").trim();
		
		key = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.key", "").trim();
		
		jsUrl = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.jsUrl", "").trim();
		
		verifyUrl = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.verifyUrl", "").trim();
	
		invisible = Boolean.parseBoolean(CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.invisible", "true"));

		jsUrl = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.jsUrl", "https://www.google.com/recaptcha/api.js").trim();
		
		verifyUrl = CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.verifyUrl", "https://www.google.com/recaptcha/api/siteverify").trim();

		if(!isIPIgnore && options.get("org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.ip.ignore") != null)
	        for (String ipMask : (Set<String>)options.get("org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.ip.ignore"))
	        	try{
	        		SubnetUtils su=new SubnetUtils(ipMask);
	        		su.setInclusiveHostCount(true);
		        	if (su.getInfo().isInRange(getHttpServletRequest().getRemoteAddr())){
		        		isIPIgnore=true;
		        		try{
	        				setUserSessionProperty("org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.ignore.range", ipMask);
	        			}catch(AuthLoginException e){}
		        		break;
		        	}
        	}catch (Throwable e) {
	        		debug.error("invalid {}: {}",ipMask,e.getMessage());
		}
		
		setForceCallbacksRead(true);

	}
	
	boolean userProcessed=false;
	String username=null;
	String in_code=null;
	Integer module_pwdCT=0;
	
	@SuppressWarnings("unchecked")
	@Override
	public int process(Callback[] in_callbacks, int state) throws LoginException {
		if (getHttpServletRequest()==null)
			return ISAuthConstants.LOGIN_IGNORE;
		
		getHttpServletRequest().setAttribute("g-recaptcha-sitekey", key);
		getHttpServletRequest().setAttribute("g-recaptcha-js-url", jsUrl);
		getHttpServletRequest().setAttribute("g-recaptcha-invisible", invisible);
		
		
		if (in_callbacks.length!=0){
			Integer CT=0;
			Integer pwdCT=module_pwdCT;
			if(state == ISAuthConstants.LOGIN_START) {
				in_code=null;
				userProcessed=false;
			}
			
			//get username, password and token from callbacks
			for (Callback callback : in_callbacks){ 
				if (callback instanceof NameCallback && !userProcessed && !StringUtils.isBlank(((NameCallback)callback).getName())){
					username=((NameCallback)callback).getName();
					if (StringUtils.isNotBlank(username))
						sharedState.put(getUserKey(), username);
					replaceCallback(state, CT, new NameCallback(((NameCallback)callback).getPrompt(), ((NameCallback)callback).getName()));
					userProcessed=true;
				}else if (callback instanceof HiddenValueCallback && ((HiddenValueCallback)callback).getValue()!=null){
					in_code=((HiddenValueCallback)callback).getValue();//last hidden value is recaptcha token
					pwdCT++;
				}
				CT++;
			}
			pwdCT--;
			
			if(System.getProperty("test.ReCaptcha") !=null	
					&& StringUtils.equalsIgnoreCase(System.getProperty("test.ReCaptcha"), in_code)) //for test
				return ISAuthConstants.LOGIN_IGNORE;
			
			if(isIPIgnore) //org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.ip.ignore
				return ISAuthConstants.LOGIN_IGNORE;
			
			if(validateRecaptcha(in_code)) 
				return ISAuthConstants.LOGIN_IGNORE;
			
		}
		
		if(state == ISAuthConstants.LOGIN_START) {
			TextOutputCallback scriptCallback = getScriptCallback();
			replaceCallback(state, 1, scriptCallback);
		}
		
		
		return state;
	}
	
	private TextOutputCallback getScriptCallback() {
		return new ScriptTextOutputCallback(""
				+ "if (window.$ && window.require) { \n"
				+ "	$('#recaptcha-container').attr('data-sitekey', '"+key+"');\n"
				+ "	require(['"+jsUrl+"'], function() {});\n"
				+ "}");
	}
	
	boolean validateRecaptcha(String token) throws AuthLoginException {
		boolean result = true;
		try {
			final HttpPost httpost = new HttpPost(verifyUrl);
			
			final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("secret", secret));
			nvps.add(new BasicNameValuePair("response", token));
			nvps.add(new BasicNameValuePair("remoteip", getHttpServletRequest().getRemoteAddr()));
			
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			
			final String responseBody;
			try (CloseableHttpResponse response = httpClient.execute(httpost)){
				responseBody=EntityUtils.toString(response.getEntity(),"UTF-8");
			}
			final JSONObject jsonResponse = new JSONObject(responseBody);
			result = jsonResponse.getBoolean("success");
			if(result) {
				AuthD.getSession(new SessionID(getSessionId())).setObject(ReCaptcha.class.getName().concat(".passed") ,true);
				setUserSessionProperty(ReCaptcha.class.getName().concat(".passed"),"1");
			}else {
				setUserSessionProperty(ReCaptcha.class.getName().concat(".error"),jsonResponse.toString());
				debug.error("failed validation {}: {} request=({})", sharedState.get(getUserKey()),jsonResponse.toString(), Dump.toString(getHttpServletRequest()));
			}
		} catch (Exception e) {
			AuthD.getSession(new SessionID(getSessionId())).setObject(ReCaptcha.class.getName().concat(".ignored.connection-error") ,true);
			setUserSessionProperty(ReCaptcha.class.getName().concat(".ignored.connection-error"),e.getMessage()==null?e.toString():e.getMessage());
			debug.error("ignore validation {}: {} request=({})", sharedState.get(getUserKey()),e.getMessage()==null?e.toString():e.getMessage(), Dump.toString(getHttpServletRequest()));
			return true;
		}
		return result;
	}

	@Override
	public Principal getPrincipal() {
		return null;
	}
}
