/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openam.oauth2.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.oauth2.OAuthTokenStore;

import javax.inject.Inject;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TokenResource implements CollectionResourceProvider {

    private OAuthTokenStore tokenStore;

    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;
    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

    private final IdentityManager identityManager;

    @Inject
    public TokenResource(final OAuthTokenStore tokenStore, final IdentityManager identityManager) {
        this.tokenStore = tokenStore;
        this.identityManager = identityManager;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler){
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler){
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest createRequest, ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Create is not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler){
        //only admin can delete
        AMIdentity uid = null;
        try {
        	//first check if SSOToken is valid
        	uid = getUid(context);

        	JsonValue response = null;
            try {
            	response = tokenStore.read(resourceId);
                if (response == null){
                    throw new NotFoundException("Token Not Found", null);
                }
                Set<String> usernameSet = (Set<String>)response.get(OAuth2Constants.CoreTokenParams.USERNAME).getObject();
                String username= null;
                if (usernameSet != null && !usernameSet.isEmpty()){
                    username = usernameSet.iterator().next();
                }
                if(username == null || username.isEmpty()){
                    throw new PermanentException(404, "Not Found", null);
                }
                
                Set<String> grantTypes = (Set<String>) response.get(OAuth2Constants.Params.GRANT_TYPE).getObject();
                String grantType = null;
                if (grantTypes != null && !grantTypes.isEmpty()){
                    grantType = grantTypes.iterator().next();
                }
                
                if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS)) {
                    tokenStore.delete(resourceId);
                } else {
                    Set<String> realms = (Set<String>) response.get(OAuth2Constants.CoreTokenParams.REALM).getObject();
                    String realm = null;
                    if (realms != null && !realms.isEmpty()){
                        realm = realms.iterator().next();
                    }
                    AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                    if (uid.equals(uid2) || uid.equals(adminUserId)) {
                        tokenStore.delete(resourceId);
                    } else {
                        throw new PermanentException(401, "Unauthorized", null);
                    }
                }
            } catch (CoreTokenException e) {
                throw new ServiceUnavailableException(e.getMessage(),e);
            }
            Map< String, String> responseVal = new HashMap< String, String>();
            responseVal.put("success", "true");
            response = new JsonValue(responseVal);
            Resource resource = new Resource(resourceId, "1", response);
            handler.handleResult(resource);
        } catch (ResourceException e){
            handler.handleError(e);
        } catch (SSOException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (IdRepoException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (UnauthorizedClientException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Patch is not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest queryRequest, QueryResultHandler handler){
        try{
            JsonValue response = null;
            Resource resource;
            try {
                Map<String, Object> query = new HashMap<String, Object>();
                String id = queryRequest.getQueryId();

                //get uid of submitter
                AMIdentity uid;
                try {
                    uid = getUid(context);
                    if (!uid.equals(adminUserId)){
                        query.put(OAuth2Constants.CoreTokenParams.USERNAME, uid.getName());
                    } else {
                        query.put(OAuth2Constants.CoreTokenParams.USERNAME, "*");
                    }
                } catch (Exception e){
                    PermanentException ex = new PermanentException(401, "Unauthorized" ,e);
                    handler.handleError(ex);
                }

                //split id into the query fields
                String[] queries = id.split("\\,");
                for (String q: queries){
                    String[] params = q.split("=");
                    if (params.length == 2){
                        query.put(params[0], params[1]);
                    }
                }

                response = tokenStore.query(query);
            } catch (CoreTokenException e) {
                throw new ServiceUnavailableException(e.getMessage(),e);
            }
            resource = new Resource("result", "1", response);
            JsonValue value = resource.getContent();
            Set<HashMap<String,Set<String>>> list = (Set<HashMap<String,Set<String>>>) value.getObject();
            Resource res = null;
            JsonValue val = null;
            if (list != null && !list.isEmpty() ){
                for (HashMap<String,Set<String>> entry : list){
                    val = new JsonValue(entry);
                    res = new Resource("result", "1", val);
                    handler.handleResource(res);
                }
            }
            handler.handleResult(new QueryResult());
        } catch (ResourceException e){
            handler.handleError(e);
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler){

        AMIdentity uid = null;
        String username = null;
        try {
        	//first check if SSOToken is valid
        	uid = getUid(context);
        	
        	JsonValue response;
            Resource resource;
            try {
                response = tokenStore.read(resourceId);
            } catch (CoreTokenException e) {
                throw new NotFoundException("Token Not Found", e);
            }
            if (response == null){
                throw new NotFoundException("Token Not Found", null);
            }

            Set<String> grantTypes = (Set<String>) response.get(OAuth2Constants.Params.GRANT_TYPE).getObject();
            String grantType = null;
            if (grantTypes != null && !grantTypes.isEmpty()){
                grantType = grantTypes.iterator().next();
            }
            
            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS)) {
            	resource = new Resource(OAuth2Constants.Params.ID, "1", response);
            	handler.handleResult(resource);
            } else {
                Set<String> realms = (Set<String>) response.get(OAuth2Constants.CoreTokenParams.REALM).getObject();
                String realm = null;
                if (realms != null && !realms.isEmpty()){
                    realm = realms.iterator().next();
                }
            
                Set<String> usernameSet = (Set<String>)response.get(OAuth2Constants.CoreTokenParams.USERNAME).getObject();
                if (usernameSet != null && !usernameSet.isEmpty()){
                username = usernameSet.iterator().next();
                }
                if(username == null || username.isEmpty()){
                    throw new PermanentException(404, "Not Found", null);
                }
                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                if (uid.equals(adminUserId) || uid.equals(uid2)){
                    resource = new Resource(OAuth2Constants.Params.ID, "1", response);
                    handler.handleResult(resource);
                } else {
                    throw new PermanentException(401, "Unauthorized" ,null);
                }
            }
        } catch (ResourceException e){
            handler.handleError(e);
        } catch (SSOException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (IdRepoException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (UnauthorizedClientException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Update is not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * Returns TokenID from headers
     *
     * @param context ServerContext which contains the headers.
     * @return String with TokenID
     */
    private String getCookieFromServerContext(ServerContext context) {
        return RestUtils.getCookieFromServerContext(context);
    }

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException, UnauthorizedClientException {
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return identityManager.getResourceOwnerIdentity(token.getProperty("UserToken"), token.getProperty("Organization"));
    }

}
