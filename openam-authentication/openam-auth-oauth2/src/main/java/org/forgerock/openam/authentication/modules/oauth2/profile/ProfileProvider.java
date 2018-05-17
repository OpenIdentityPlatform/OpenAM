package org.forgerock.openam.authentication.modules.oauth2.profile;

import javax.security.auth.login.LoginException;

import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;

public interface ProfileProvider {
	public String getProfile(OAuthConf config, String token) throws LoginException;
}
