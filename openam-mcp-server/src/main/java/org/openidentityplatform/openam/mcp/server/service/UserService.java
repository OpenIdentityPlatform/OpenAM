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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.service;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.openidentityplatform.openam.mcp.server.model.SearchResponseDTO;
import org.openidentityplatform.openam.mcp.server.model.User;
import org.openidentityplatform.openam.mcp.server.model.UserDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService extends OpenAMAbstractService {
    public UserService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        super(openAMRestClient, openAMConfig);
    }

    @Tool(name = "get_users", description = "Returns OpenAM user list from the default (root) realm")
    public List<User> getUsers(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                               @ToolParam(required = false, description = "Username filter") String filter) {

        realm = getRealmOrDefault(realm);

        String queryFilter = "true";
        if(filter != null) {
            queryFilter= "cn sw \"".concat(filter).concat("\"");
        }
        String uri = String.format("/json/realms/%s/users?_queryFilter=%s", realm, queryFilter);
        String tokenId = getTokenId();
        SearchResponseDTO<UserDTO> userSearchResponse  = openAMRestClient.get().uri(uri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return userSearchResponse.result().stream().map(User::new).collect(Collectors.toList());
    }

    private static final Map<String, String> ATTR_MAP = Map.of("familyName", "sn",
            "givenName", "givenName",
            "name", "cn",
            "mail", "mail",
            "phone", "telephoneNumber");

    @Tool(name = "set_user_attribute", description = "Sets the attribute value for a user")
    public User setUserAttribute(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                                 @ToolParam(description = "username") String username,
                                 @ToolParam(description = "user attribute name") String attribute,
                                 @ToolParam(description = "user attribute value") String value) {

        realm = getRealmOrDefault(realm);

        if(!ATTR_MAP.containsKey(attribute)) {
            throw new RuntimeException(String.format("invalid attribute: %s; allowed values %s", attribute, ATTR_MAP.keySet()));
        }
        String tokenId = getTokenId();

        Map<String, String> requestBody = Map.of(ATTR_MAP.get(attribute), value);
        String uri = String.format("/json/realms/%s/users/%s", realm, username);
        UserDTO user = openAMRestClient.put().uri(uri).body(requestBody)
                .header(openAMConfig.tokenHeader(), tokenId)
                .header("Accept-API-Version","resource=2.0, protocol=1.0")
                .retrieve()
                .body(UserDTO.class);
        return new User(user);
    }


    @Tool(name = "set_user_password", description = "Sets the password for a user")
    public User setUserPassword(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                                 @ToolParam(description = "username") String username,
                                 @ToolParam(description = "user password") String password) {

        realm = getRealmOrDefault(realm);

        String tokenId = getTokenId();

        Map<String, String> requestBody = Map.of("userpassword", password);
        String uri = String.format("/json/realms/%s/users/%s?_action=changePassword", realm, username);
        UserDTO user = openAMRestClient.put().uri(uri).body(requestBody)
                .header(openAMConfig.tokenHeader(), tokenId)
                .header("Accept-API-Version","resource=2.0, protocol=1.0")
                .retrieve()
                .body(UserDTO.class);
        return new User(user);
    }

    @Tool(name = "create_user", description = "Creates a new user")
    public User createUser(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                                 @ToolParam(description = "Username (login)") String userName,
                                 @ToolParam(description = "Password (min length 8)") String password,
                                 @ToolParam(required = false, description = "User family name") String familyName,
                                 @ToolParam(required = false, description = "User given name") String givenName,
                                 @ToolParam(required = false, description = "Name") String name,
                                 @ToolParam(required = false, description = "Email") String mail,
                                 @ToolParam(required = false, description = "Phone number") String phone
    ) {

        realm = getRealmOrDefault(realm);

        Map<String, String> userProps = new HashMap<>();
        userProps.put("username", userName);
        userProps.put("userpassword", password);
        if(familyName != null) {
            userProps.put(ATTR_MAP.get("familyName"), familyName);
        }
        if(givenName != null) {
            userProps.put(ATTR_MAP.get("givenName"), givenName);
        }
        if(name != null) {
            userProps.put(ATTR_MAP.get("name"), name);
        }
        if(mail != null) {
            userProps.put(ATTR_MAP.get("mail"), mail);
        }
        if(phone != null) {
            userProps.put(ATTR_MAP.get("phone"), phone);
        }

        String tokenId = getTokenId();

        String uri = String.format("/json/realms/%s/users/?_action=create", realm);
        UserDTO user = openAMRestClient.post().uri(uri).body(userProps)
                .header(openAMConfig.tokenHeader(), tokenId)
                .header("Accept-API-Version","resource=2.0, protocol=1.0")
                .retrieve()
                .body(UserDTO.class);
        return new User(user);
    }

    @Tool(name = "delete_user", description = "Deletes a user")
    public Map<String, String> deleteUser(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                           @ToolParam(description = "Username (login)") String username) {

        realm = getRealmOrDefault(realm);

        String tokenId = getTokenId();

        String uri = String.format("/json/realms/%s/users/%s", realm, username);
        return openAMRestClient.delete().uri(uri)
                .header(openAMConfig.tokenHeader(), tokenId)
                .header("Accept-API-Version","resource=2.0, protocol=1.0")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
