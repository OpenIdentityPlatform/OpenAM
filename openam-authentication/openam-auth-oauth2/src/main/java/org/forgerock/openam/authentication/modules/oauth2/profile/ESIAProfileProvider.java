package org.forgerock.openam.authentication.modules.oauth2.profile;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.authentication.modules.oauth2.HttpRequestContent;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.authentication.modules.oauth2.service.ESIAServiceUrlProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESIAProfileProvider implements ProfileProvider {

	final static Logger logger = LoggerFactory.getLogger(ESIAProfileProvider.class);
	
	private static final ProfileProvider INSTANCE = new ESIAProfileProvider();
	
	public static final String ESIA_ORG_SCOPE = "[esia-org-scope]";
	
	public static final String ESIA_ORG_INFO_URL = "[esia-org-info-url]";
	
	static final Pattern ORG_ID_PATTERN = Pattern.compile("(\\d{1,})$");
	
	public static ProfileProvider getInstance() {
		return INSTANCE;
	}
	
	@Override
	public String getProfile(OAuthConf config, String token) throws LoginException {
		String[] jwtParts = token.split("\\.", -1);
		
		String oid = "";
		try {
			JSONObject jwtJson = new JSONObject(new String(Base64.decodeBase64(jwtParts[1])));
			oid = jwtJson.getString("urn:esia:sbj_id");
		} catch(Exception e) {	
		}
		
		String profileStr = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl().concat("/").concat(oid), "Bearer " + token,
                config.getProfileServiceGetParameters());
		
		String cttsStr = null;
		
		try {
			cttsStr = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl().concat("/").concat(oid).concat("/ctts?embed=(elements)"), "Bearer " + token,
                config.getProfileServiceGetParameters());
		} catch (Exception e) {}
		
		String docsStr = null;
		try {
			docsStr = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl().concat("/").concat(oid).concat("/docs?embed=(elements)"), "Bearer " + token,
                config.getProfileServiceGetParameters());
		} catch (Exception e) {}
		
		String addrStr = null;
		try {
			addrStr = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl().concat("/").concat(oid).concat("/addrs?embed=(elements)"), "Bearer " + token,
                config.getProfileServiceGetParameters());
		} catch (Exception e) {}
		
		String orgsStr = null;
		try { 
			orgsStr = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl().concat("/").concat(oid).concat("/orgs"), "Bearer " + token,
                config.getProfileServiceGetParameters());
		} catch (Exception e) {}
		
		
		if(orgsStr != null && config.getCustomProperties().containsKey(ESIA_ORG_SCOPE) 
				&& StringUtils.isNotBlank(config.getCustomProperties().get(ESIA_ORG_SCOPE))
				&& config.getCustomProperties().containsKey(ESIA_ORG_INFO_URL) 
				&& StringUtils.isNotBlank(config.getCustomProperties().get(ESIA_ORG_INFO_URL))) {
			
			ESIAServiceUrlProvider provider = new ESIAServiceUrlProvider();
			
			try {
				JSONObject orgsJson = new JSONObject(orgsStr);
				JSONArray orgsArray = orgsJson.getJSONArray("elements");
				for(int i = 0; i < orgsArray.length(); i++) {
					String orgUrl = orgsArray.getString(i); 
					final Matcher m = ORG_ID_PATTERN.matcher(orgUrl);
					String orgId = "";
					if(!m.find()) {
						continue;
			        }
					orgId = m.group();
					String[] scopes = StringUtils.split(config.getCustomProperties().get(ESIA_ORG_SCOPE), " ");
					for(int j = 0; j< scopes.length; j ++) {
						scopes[j] = MessageFormat.format("http://esia.gosuslugi.ru/{0}?org_oid={1}", scopes[j], orgId);
					}
					String scope = StringUtils.join(scopes, " ");
									
					String tokenSvcResponse = HttpRequestContent.getInstance().getContentUsingPOST(config.getTokenServiceUrl(), 
							null, 
	                 		null,
	                 		provider.getTokenServiceClientPOSTparameters(config, scope));
	                 OAuthUtil.debugMessage("OAuth.process(): token=" + tokenSvcResponse);
	                 JSONObject orgJsonToken = new JSONObject(tokenSvcResponse);
	                 String orgToken = orgJsonToken.getString("access_token");
                 

	         		String orgStr = HttpRequestContent.getInstance().getContentUsingGET(config.getCustomProperties().get(ESIA_ORG_INFO_URL).concat(orgId), "Bearer " + orgToken,
	                        config.getProfileServiceGetParameters());
	         		 
	         		JSONObject orgJson = new JSONObject(orgStr);
	         		orgsArray.put(i, orgJson);
				}
				
				orgsStr = orgsJson.toString();
				
			} catch (JSONException e) {
				logger.warn("error embed orgs profile: {}", orgsStr, e.toString());
			}
		}
						
		return buildProfile(oid, profileStr, cttsStr, docsStr, addrStr, orgsStr);
	}
	
	public String buildProfile(String oid, String profileStr, String cttsStr, String docsStr, String addrStr, String orgsStr)  {
		
		String email = "";
		String phone = "";
		String workEmail = "";
		String homePhone = "";
		try {
			JSONObject profile = new JSONObject(profileStr);
			
			
			if(cttsStr != null) {
				JSONObject contactsJsonObj = new JSONObject(cttsStr);
				JSONArray contacts =  contactsJsonObj.getJSONArray("elements");
				
				for(int i = 0; i < contacts.length(); i++) {
					JSONObject contact = contacts.getJSONObject(i);
					if(!contact.has("type") || !contact.has("value"))
						continue;
					
					if("EML".equals(contact.getString("type")) && StringUtils.isBlank(email)) {
						email = contact.getString("value");
					}
					if("MBT".equals(contact.getString("type"))&& StringUtils.isBlank(phone)) {
						phone = contact.getString("value").replaceAll("[^\\d]", "");
					}
					if("CEM".equals(contact.getString("type"))&& StringUtils.isBlank(workEmail)) {
						workEmail = contact.getString("value");
					}
					if("PHN".equals(contact.getString("type"))&& StringUtils.isBlank(homePhone)) {
						homePhone = contact.getString("value").replaceAll("[^\\d]", "");
					}
				}
				
				//put for attribute mapping
				profile.put("oid", oid);
				profile.put("phone", phone);
				profile.put("email", email);
				profile.put("workEmail", workEmail);
				profile.put("homePhone", homePhone);
				
				//add full contacts info
				profile.put("ctts", contactsJsonObj);
			}
			if(docsStr != null)
				try {
					JSONObject docs = new JSONObject(docsStr);
					profile.put("docs", docs);
				} catch (JSONException e) {
					logger.warn("error embed docs profile: {}", docsStr, e.toString());
				}
			
			if(orgsStr != null)
				try {
					JSONObject orgs = new JSONObject(orgsStr);
					profile.put("orgs", orgs);
				} catch (JSONException e) {
					logger.warn("error embed orgs profile: {}", orgsStr, e.toString());
				}
			
			if(addrStr != null)
				try {
					JSONObject addrs = new JSONObject(addrStr);
					profile.put("addrs", addrs);
				} catch (JSONException e) {
					logger.warn("error embed addrs profile: {}", addrStr, e.toString());
				}
			
			if(orgsStr != null)
				try {
					JSONObject orgs = new JSONObject(orgsStr);
					profile.put("orgs", orgs);
				} catch (JSONException e) {
					logger.warn("error embed orgs profile: {}", orgsStr, e.toString());
				}
			
			return profile.toString();
			
		} catch(Exception e) {
			logger.warn("error convert profile {} to plain: {}", profileStr, e.toString());
		}
		
		return profileStr;
	}

}
