package org.openidentityplatform.openam.authentication.modules.recaptcha;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import ru.org.openam.httpdump.Dump;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

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
	
	final static Integer connectTimeout = Integer.parseInt(SystemProperties.get(ReCaptcha.class.getName()+".timeout","500"));
	
	final static RequestConfig requestConfig = RequestConfig.custom()
		    .setSocketTimeout(connectTimeout)
		    .setConnectTimeout(connectTimeout)
		    .setConnectionRequestTimeout(connectTimeout)
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
	
	private boolean invisible = false;

	
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
	
		invisible = Boolean.parseBoolean(CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.recaptcha.ReCaptcha.invisible", "false"));
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
				}else if (callback instanceof PasswordCallback && ((PasswordCallback)callback).getPassword()!=null){
					sharedState.put(getPwdKey().concat(pwdCT==0?"":pwdCT.toString()), new String(((PasswordCallback)callback).getPassword()));
					replaceCallback(state, CT, callback);
					in_code=new String(((PasswordCallback)callback).getPassword());//last password in is recaptcha token
					pwdCT++;
				}
				CT++;
			}
			pwdCT--;
			
			if(validateRecaptcha(in_code)) 
				return ISAuthConstants.LOGIN_IGNORE;
			
		}
		
		return state;
	}
	
	boolean validateRecaptcha(String token) throws AuthLoginException {
		boolean result = true;
		try {
			HttpPost httpost = new HttpPost(verifyUrl);
			
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("secret", secret));
			nvps.add(new BasicNameValuePair("response", token));
			nvps.add(new BasicNameValuePair("remoteip", getHttpServletRequest().getRemoteAddr()));
			
			httpost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = httpClient.execute(httpost); 
			String responseBody=EntityUtils.toString(response.getEntity(),"UTF-8");
			JSONObject jsonResponse = new JSONObject(responseBody);
			result = jsonResponse.getBoolean("success");
			if(result) {
				AuthD.getSession(new SessionID(getSessionId())).setObject(ReCaptcha.class.getName().concat(".passed") ,true);
				setUserSessionProperty(ReCaptcha.class.getName().concat(".passed"),"1");
			}
		} catch (Exception e) {
			AuthD.getSession(new SessionID(getSessionId())).setObject(ReCaptcha.class.getName().concat(".ignored.connection-error") ,true);
			setUserSessionProperty(ReCaptcha.class.getName().concat(".ignored.connection-error"),"1");
			debug.error("Exception ocurred while validating reCaptcha: error: {0} request=({1})", e.getMessage(), Dump.toString(getHttpServletRequest()));
			return true;
		}
		return result;
	}

	@Override
	public Principal getPrincipal() {
		return null;
	}
}
