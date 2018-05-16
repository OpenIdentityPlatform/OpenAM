package org.forgerock.openam.authentication.modules.oauth2.profile;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;

public class ProfileProviderFactory {
	public static ProfileProvider getProfileProvider(OAuthConf config) {
		if(StringUtils.defaultString(config.getProfileServiceUrl()).contains("api.ok.ru"))
			return OdnoklassnikiProvider.getInstance();
		if(StringUtils.defaultString(config.getProfileServiceUrl()).contains("api.vk.com"))
			return VkontakteProvider.getInstance();
		return DefaultProfileProvider.getInstance();
	}
}
