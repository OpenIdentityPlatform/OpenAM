package org.forgerock.openam.authentication.modules.oauth2.profile;

import javax.security.auth.login.LoginException;

import org.forgerock.openam.authentication.modules.oauth2.HttpRequestContent;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;

public class DefaultProfileProvider implements ProfileProvider {

	private static final ProfileProvider INSTANCE = new DefaultProfileProvider();
	
	public static ProfileProvider getInstance() {
		return INSTANCE;
	}
	
	@Override
	public String getProfile(OAuthConf config, String token) throws LoginException {
		return HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl(), "Bearer " + token,
                config.getProfileServiceGetParameters());
	}

}
