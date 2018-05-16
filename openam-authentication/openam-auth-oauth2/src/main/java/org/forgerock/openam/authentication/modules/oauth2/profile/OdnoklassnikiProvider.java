package org.forgerock.openam.authentication.modules.oauth2.profile;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.CipherInputStream;
import javax.security.auth.login.LoginException;

import org.apache.commons.collections.CollectionUtils;
import org.forgerock.openam.authentication.modules.oauth2.HttpRequestContent;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;

public class OdnoklassnikiProvider implements ProfileProvider {

	private static final ProfileProvider INSTANCE = new OdnoklassnikiProvider();
	
	public static ProfileProvider getInstance() {
		return INSTANCE;
	}
	
	public static final String PUBLIC_KEY = "[ok-public-key]";
	
	@Override
	public String getProfile(OAuthConf config, String token) throws LoginException {
		
		//profile servce url
		String profileServiceUrl = "https://api.ok.ru/api/users/getCurrentUser";
		String publicKey = config.getCustomProperties().get(PUBLIC_KEY);
		String privateKey = config.getClientSecret();
		Map<String, String> parameters = new HashMap<>();
		parameters.put("application_key", publicKey);
		parameters.put("format", "JSON");
		System.err.println(profileServiceUrl);
		try {
			signRequest(token, parameters, privateKey);
		} catch(Exception e) { //TODO process signing exception
			e.printStackTrace();
		}
		parameters.put("access_token", token);
		return HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl(), null,
				parameters);
	}
	
    public void signRequest(String accessToken, Map<String, String> params, String api_secret) throws UnsupportedEncodingException {
        //sig = lower(md5( sorted_request_params_composed_string + md5(access_token + application_secret_key)))
  
        final String tokenDigest = md5(accessToken + api_secret);


        List<String> paramKeys = new ArrayList<>(params.keySet());
        Collections.sort(paramKeys);

        final StringBuilder stringParams = new StringBuilder();
        for (String paramKey : paramKeys) {
            stringParams.append(paramKey)
                    .append('=')
                    .append(params.get(paramKey));
        }

        final String sigSource = URLDecoder.decode(stringParams.toString(), "UTF-8") + tokenDigest;
        params.put("sig", md5(sigSource).toLowerCase());
         
    }

    public static String md5(String orgString) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] array = md.digest(orgString.getBytes(Charset.forName("UTF-8")));
            final Formatter builder = new Formatter();
            for (byte b : array) {
                builder.format("%02x", b);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is unsupported?", e);
        }
    }

}
