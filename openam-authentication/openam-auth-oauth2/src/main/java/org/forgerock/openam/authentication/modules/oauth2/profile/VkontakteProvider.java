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
 * Copyright 2018-2024 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.oauth2.profile;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.forgerock.openam.authentication.modules.oauth2.HttpRequestContent;
import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;

public class VkontakteProvider implements ProfileProvider {
	
	private static Debug DEBUG = Debug.getInstance("amAuthOAuth2");

	public static final String VK_API_VERSION = "[vk-api-version]";

	private static final ProfileProvider INSTANCE = new VkontakteProvider();
	
	public static ProfileProvider getInstance() {
		return INSTANCE;
	}
	
	private static String PROFILE_FIELDS = "photo_id,verified,sex,bdate,city,country,home_town,"
			+ "has_photo,photo_50,photo_100,photo_200_orig,photo_200,photo_400_orig,photo_max,"
			+ "photo_max_orig,online,domain,has_mobile,contacts,site,education,universities,"
			+ "schools,status,last_seen,followers_count,occupation,nickname,relatives,"
			+ "relation,personal,connections,exports,wall_comments,activities,interests,music,movies,"
			+ "tv,books,games,about,quotes,can_post,can_see_all_posts,can_see_audio,"
			+ "can_write_private_message,can_send_friend_request,is_favorite,is_hidden_from_feed,"
			+ "timezone,screen_name,maiden_name,crop_photo,is_friend,friend_status,career,military,"
			+ "blacklisted,blacklisted_by_me";
	
	@Override
	public String getProfile(OAuthConf config, String token) throws LoginException {
		
		//profile servce url
		Map<String, String> parameters = new HashMap<>();
		parameters.put("fields", PROFILE_FIELDS);
		parameters.put("access_token", token);
		parameters.put("v", "5.199");
		if(config.getCustomProperties().get(VK_API_VERSION) != null) {
			parameters.put("v", config.getCustomProperties().get(VK_API_VERSION));
		}
		String jsonResponse = HttpRequestContent.getInstance().getContentUsingGET(config.getProfileServiceUrl(), null,
				parameters);
		
		try {
			JSONObject jsonObject = new JSONObject(jsonResponse).getJSONArray("response").getJSONObject(0);
			jsonObject.put("name", (jsonObject.getString("last_name") + " " + jsonObject.getString("first_name")).trim());
			
			return jsonObject.toString();
		} catch (JSONException e) {
			throw new AuthLoginException(e); 
		}
	}
	
 

}
