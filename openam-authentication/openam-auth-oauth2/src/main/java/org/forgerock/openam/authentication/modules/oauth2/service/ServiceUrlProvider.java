package org.forgerock.openam.authentication.modules.oauth2.service;

import java.util.Map;

import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;

import com.sun.identity.authentication.spi.AuthLoginException;

public interface ServiceUrlProvider {
	String getServiceUri(OAuthConf config, String originalUrl, String state) throws AuthLoginException;
	
	 Map<String, String> getTokenServicePOSTparameters(OAuthConf config, String code, String authServiceURL)
	            throws AuthLoginException;

	Map<String, String> getTokenServiceGETparameters(OAuthConf oAuthConf, String code, String authServiceURL)
			throws AuthLoginException;
}
