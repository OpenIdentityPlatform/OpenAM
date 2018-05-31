package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;
import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.authentication.modules.oauth2.profile.DefaultProfileProvider;
import org.forgerock.openam.authentication.modules.oauth2.profile.OdnoklassnikiProvider;
import org.forgerock.openam.authentication.modules.oauth2.profile.ProfileProvider;
import org.forgerock.openam.authentication.modules.oauth2.profile.ProfileProviderFactory;
import org.forgerock.openam.authentication.modules.oauth2.profile.VkontakteProvider;
import org.testng.annotations.Test;


public class ProfileProviderTest {
	@Test
	public void testFactoryProvider() {
		OAuthConf config = new OAuthConf();
		ProfileProvider defaultProvider = ProfileProviderFactory.getProfileProvider(config);
		assertEquals(defaultProvider.getClass(), DefaultProfileProvider.class);
		
		Map<String, Set<String>> okConfigMap = new HashMap<>();
		okConfigMap.put(KEY_PROFILE_SERVICE, Collections.singleton("https://api.ok.ru/fb.do"));

		//avoid npe
		okConfigMap.put(KEY_ACCOUNT_MAPPER_CONFIG, Collections.singleton("local=source"));
		okConfigMap.put(KEY_ATTRIBUTE_MAPPER_CONFIG, Collections.singleton("local=source"));
//		okConfigMap.put(KEY_CUSTOM_PROPERTIES, Collections.singleton("[]="));
		OAuthConf okConfig = new OAuthConf(okConfigMap);
		
		ProfileProvider okProvider = ProfileProviderFactory.getProfileProvider(okConfig);
		assertEquals(okProvider.getClass(), OdnoklassnikiProvider.class);
	
		Map<String, Set<String>> vkConfigMap = new HashMap<>();
		vkConfigMap.put(KEY_PROFILE_SERVICE, Collections.singleton("https://api.vk.com/method/users.get"));

		//avoid npe
		vkConfigMap.put(KEY_ACCOUNT_MAPPER_CONFIG, Collections.singleton("local=source"));
		vkConfigMap.put(KEY_ATTRIBUTE_MAPPER_CONFIG, Collections.singleton("local=source"));
//		okConfigMap.put(KEY_CUSTOM_PROPERTIES, Collections.singleton("[]="));
		OAuthConf vkConfig = new OAuthConf(vkConfigMap);
		
		ProfileProvider vkProvider = ProfileProviderFactory.getProfileProvider(vkConfig);
		assertEquals(vkProvider.getClass(), VkontakteProvider.class);
	}
	
	
	
}
