/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.oauth2.utils;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.forgerockrest.RestDispatcher;
import org.restlet.Client;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.ClientResource;

import java.security.AccessController;
import java.util.*;

/**
 * A Collection of OAuth2 Rest Helper Methods
 */
public class OAuth2RestUtils {

    // Client Variables
    private static final String AGENT_TYPE = "AgentType";
    private static final String DEVICE_STATUS = "sunIdentityServerDeviceStatus";
    private static String CLIENT_TYPE = "Public";
    private static final String USER_PASSWORD = "userpassword";
    private static String DEFAULT_PASSWORD = "cangetin";

    // Administrative Variables
    private static  SSOToken token;
    private static ServiceConfigManager sm;
    static private final String TOKEN_INFO = "/oauth2/tokeninfo";

    // Service Default Variables
    private static String refreshTokenLifetime = "86400";
    private static String accessCodeLifetime = "10";
    private static String accessTokenLifetime = "900";
    private static String issueRefreshToken = "true";
    private static String scopeImplementationClass = "org.forgerock.openam.oauth2.provider.impl.ScopeImpl";


    static {
        token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Constructor used for testing OAuth2RestUtils
     * @param sm service configuration manager
     */
    public OAuth2RestUtils(ServiceConfigManager sm, SSOToken token){
        this.sm = sm;
        this.token = token;
    }

    /**
     * Create an OAuth2 Client in specified realm
     * @param clientID name of the client to be created
     * @param realm realm in which client will be created
     * @return OAuth2 Client AMIdentity if successful; null if otherwise
     */
    public static AMIdentity createOAuth2Client(String clientID, String realm) throws NotFoundException {

        AMIdentity amIdent = null;
        Set<AMIdentity> results = Collections.EMPTY_SET;

        try {
            AMIdentityRepository idRepo = new AMIdentityRepository(
                    (SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance()), realm);
            Map<String, Set<String>> attrs = new HashMap<String,Set<String>>();

            // Check to see if client exists already
            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);

            IdSearchResults searchResults = idRepo.searchIdentities(IdType.AGENT,clientID, idsc);

            if (searchResults != null) {
                results = searchResults.getSearchResults();
                if(results.size() == 0 ){
                    // Do nothing, skip to create identity
                } else if(results != null && results.size() == 1  ) {
                    // Identity found
                    return results.iterator().next();
                } else if (results == null || results.size() != 1) {
                    throw new IdRepoException("OAuth2" + ".getIdentity : More than one user found");
                }

            }

            // Client ID
            Set<String> temp = new HashSet<String>();
            temp.add("OAuth2Client");
            attrs.put(AGENT_TYPE, temp);

            temp = new HashSet<String>();
            temp.add("Active");
            attrs.put(DEVICE_STATUS, temp);

            // Client Secret
            temp = new HashSet<String>();
            temp.add(DEFAULT_PASSWORD);
            attrs.put(USER_PASSWORD, temp);

            // Client Type - Public
            temp = new HashSet<String>();
            temp.add("Public");
            attrs.put(OAuth2Constants.OAuth2Client.CLIENT_TYPE, temp);

            amIdent = idRepo.createIdentity(IdType.AGENTONLY, clientID, attrs);
            return amIdent;
        } catch (Exception e){
            RestDispatcher.debug.error("IdentityResource.createOAuth2Client :: Cannot create OAuth2 Client " + e);
            throw new NotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Creates an OAuth2 Service in the realm provided
     * @param sm Serivce Configuration Manager for the OAuth2 provider
     * @param realm Realm in which to create the service
     * @throws NotFoundException
     */
    public static void createOAuth2Service(ServiceConfigManager sm, String realm) throws NotFoundException {

        try {
            // Get instances from service config manager
            Set instances = sm.getInstanceNames();

            // Check if service has been created already
            if(instances != null && !instances.isEmpty()){
                return;
            }

            // Create service attrs
            Map<String,Set<String>> attrValues = new HashMap<String, Set<String>>();
            Set<String> temp = new HashSet<String>();
            temp.add(refreshTokenLifetime);
            attrValues.put(OAuth2Constants.OAuth2ProviderService.REFRESH_TOKEN_LIFETIME_NAME, temp);
            temp = new HashSet<String>();
            temp.add(accessCodeLifetime);
            attrValues.put(OAuth2Constants.OAuth2ProviderService.AUTHZ_CODE_LIFETIME_NAME, temp);
            temp = new HashSet<String>();
            temp.add(accessTokenLifetime);
            attrValues.put(OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_LIFETIME_NAME, temp);
            temp = new HashSet<String>();
            temp.add(issueRefreshToken);
            attrValues.put(OAuth2Constants.OAuth2ProviderService.ISSUE_REFRESH_TOKEN, temp);
            temp = new HashSet<String>();
            temp.add(scopeImplementationClass);
            attrValues.put(OAuth2Constants.OAuth2ProviderService.SCOPE_PLUGIN_CLASS, temp);

            sm.createOrganizationConfig(realm,attrValues);

        } catch (Exception e){
            RestDispatcher.debug.error("IdentityResource.createOAuth2Service:: Cannot create OAuth2 Service " + e);
            throw new NotFoundException(e.getMessage(), e);
        }
    }

    /**
     * Validates the OAuth2 access token on the server
     * @param fullDeploymentURL full server url
     * @param access_token OAuth2 access_token
     * @return response from the token info endpoint
     */
    public static Response validateOAuth2Token(StringBuilder fullDeploymentURL, String access_token) throws BadRequestException {

        StringBuilder fullDepURL = fullDeploymentURL;

        try {
            if(fullDepURL == null){
                RestDispatcher.debug.error("IdentityResource.validateOAuth2Token:: Invalid Deployment URL" +
                        fullDepURL.toString());
                throw new BadRequestException("Invalid Deployment URL", null);
            }
            // Append the tokeninfo endpoint
            fullDepURL.append(TOKEN_INFO);

            // Build up request
            Reference reference = new Reference(fullDepURL.toString());
            if(access_token == null || access_token.isEmpty()){
                throw new BadRequestException("access_token not provided", null);
            }
            reference.addQueryParameter("access_token", access_token);
            Client client = new Client(new org.restlet.Context(), Protocol.HTTP);
            ClientResource clientResource = new ClientResource(reference.toUri());
            clientResource.setNext(client);

            // Get Response from Request
            clientResource.get();

            // Return the response
            return clientResource.getResponse();
        } catch (Exception e) {
            RestDispatcher.debug.error("IdentityResource.validateOAuth2Token:: " + e.getMessage() );
            throw new BadRequestException(e.getMessage(), null);
        }
    }
}
