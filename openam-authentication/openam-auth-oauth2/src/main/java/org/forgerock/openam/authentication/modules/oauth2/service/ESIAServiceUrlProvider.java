package org.forgerock.openam.authentication.modules.oauth2.service;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CLIENT_ID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CLIENT_SECRET;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CODE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_GRANT_TYPE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_REDIRECT_URI;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_SCOPE;

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.DateUtils;
import org.forgerock.openam.authentication.modules.oauth2.HttpRequestContent;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.authentication.modules.oauth2.service.esia.Signer;
import org.forgerock.openam.oauth2.OAuth2Constants;

import com.sun.identity.authentication.spi.AuthLoginException;

public class ESIAServiceUrlProvider implements ServiceUrlProvider {

	private final static String UTF_8 = "UTF-8";
	private final static SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss Z");

	final Signer signer;
	public ESIAServiceUrlProvider(String keyPath, String certPath) {
		this.signer = new Signer(keyPath, certPath);
	}
	
	@Override
	public String getServiceUri(OAuthConf config, String originalUrl, String state) throws AuthLoginException {
		String uriTemplate = config.getAuthServiceUrl().concat("?client_id={0}&client_secret={1}&redirect_uri={2}&scope={3}&response_type=code&state={4}&timestamp={5}&access_type=offline");
		String timestamp = getTimeStamp();
		
		String authUrl = "";
		try {
			
			authUrl = MessageFormat.format(uriTemplate, 
				URLEncoder.encode(config.getClientId(), UTF_8), 
				URLEncoder.encode(signer.signString(config.getScope() +timestamp+config.getClientId()+state), UTF_8),
				URLEncoder.encode(originalUrl, UTF_8),
				URLEncoder.encode(config.getScope(), UTF_8),
				URLEncoder.encode(state, UTF_8), 
				URLEncoder.encode(timestamp, UTF_8));
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return authUrl;
	}

	
	@Override
	public Map<String, String> getTokenServicePOSTparameters(OAuthConf config, String code, String authServiceURL)
			throws AuthLoginException {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
        if (code == null) {
            OAuthUtil.debugError("process: code == null");
            throw new AuthLoginException("amAuthOAuth", "authCode == null", null);
        }
        OAuthUtil.debugMessage("authentication code: " + code);
        String timestamp = getTimeStamp();
        String state = UUID.randomUUID().toString();
        try {
	        parameters.put(PARAM_CLIENT_ID, config.getClientId());
	        parameters.put(PARAM_CODE, URLEncoder.encode(code, UTF_8));
	        parameters.put(PARAM_GRANT_TYPE, OAuth2Constants.TokenEndpoint.AUTHORIZATION_CODE);
	        parameters.put(PARAM_CLIENT_SECRET, URLEncoder.encode(signer.signString(config.getScope()+timestamp+config.getClientId()+state), UTF_8));
	        parameters.put(PARAM_REDIRECT_URI, URLEncoder.encode(authServiceURL, UTF_8));
	        parameters.put(PARAM_SCOPE, URLEncoder.encode(config.getScope(), UTF_8));
	        parameters.put("state", URLEncoder.encode(state, UTF_8));
	        parameters.put("timestamp", URLEncoder.encode(timestamp, UTF_8));
	        parameters.put("token_type", "Bearer");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

        return parameters;
	}
	
       
    public Map<String, String> getTokenServiceClientPOSTparameters(OAuthConf config, 
    		String scope) {
    	
    	
		Map<String, String> parameters = new LinkedHashMap<>();
        String timestamp = getTimeStamp();
        String state = UUID.randomUUID().toString();
        try {
	        parameters.put(PARAM_CLIENT_ID, config.getClientId());
	        parameters.put("response_type", "token");
	        parameters.put(PARAM_SCOPE, URLEncoder.encode(scope, UTF_8));
	        parameters.put(PARAM_GRANT_TYPE, "client_credentials");
	        parameters.put("state", URLEncoder.encode(state, UTF_8));
	        parameters.put("timestamp", URLEncoder.encode(timestamp, UTF_8));
	        parameters.put("token_type", "Bearer");
	        parameters.put(PARAM_CLIENT_SECRET, URLEncoder.encode(signer.signString(scope+timestamp+config.getClientId()+state), UTF_8));
	        
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

        return parameters;
    }
    

	@Override
	public Map<String, String> getTokenServiceGETparameters(OAuthConf config, String code, String authServiceURL) throws AuthLoginException {
		return null;
	}
	
	static long syncOffset = 0l; //todo get sync offset
	static final ScheduledExecutorService syncTime = Executors.newSingleThreadScheduledExecutor();
	static{
		syncTime.scheduleAtFixedRate(new Runnable() {public void run() { try {
			syncOffset = getSyncOffset();
		} catch (Exception e) {
			OAuthUtil.debugWarning("ESIA error sync time: " + e.toString());
		}}},0, 5, TimeUnit.MINUTES);
	}
	
	public static void init() {} //for context listener, loads class
	
	static final String ESIA_HOST_SYNC_TIME = "https://esia.gosuslugi.ru/"; 
	
	public static long getSyncOffset() throws Exception {
		Map<String, List<String>> headers = HttpRequestContent.getInstance().getHeadersUsingHEAD(ESIA_HOST_SYNC_TIME);
		String strDate = headers.get("Date").get(0);
		Date networkDate = DateUtils.parseDate(strDate);
		return Calendar.getInstance().getTime().getTime() - networkDate.getTime();
	}
	
	String getTimeStamp() {
		Calendar cal = Calendar.getInstance();
		long timeMills = Calendar.getInstance().getTime().getTime() - syncOffset;
		cal.setTimeInMillis(timeMills);
		return format.format(cal.getTime());
	}
}
