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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.admin;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
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
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.OAuth2Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 12.0.0
 */
public class TokenResource implements CollectionResourceProvider {

    @Override
    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest createRequest, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler){
        //only admin can delete
//        AMIdentity uid = null;
//        try {
//            //first check if SSOToken is valid
//            uid = getUid(context);
//
//            JsonValue response = null;
//            try {
//                response = tokenStore.read(resourceId);
//                if (response == null){
//                    throw new NotFoundException("Token Not Found", null);
//                }
//                Set<String> usernameSet = (Set<String>)response.get(OAuth2Constants.CoreTokenParams.USERNAME).getObject();
//                String username= null;
//                if (usernameSet != null && !usernameSet.isEmpty()){
//                    username = usernameSet.iterator().next();
//                }
//                if(username == null || username.isEmpty()){
//                    throw new PermanentException(404, "Not Found", null);
//                }
//
//                Set<String> grantTypes = (Set<String>) response.get(OAuth2Constants.Params.GRANT_TYPE).getObject();
//                String grantType = null;
//                if (grantTypes != null && !grantTypes.isEmpty()){
//                    grantType = grantTypes.iterator().next();
//                }
//
//                if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
//                    tokenStore.delete(resourceId);
//                } else {
//                    Set<String> realms = (Set<String>) response.get(OAuth2Constants.CoreTokenParams.REALM).getObject();
//                    String realm = null;
//                    if (realms != null && !realms.isEmpty()){
//                        realm = realms.iterator().next();
//                    }
//                    AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
//                    if (uid.equals(uid2) || uid.equals(adminUserId)) {
//                        tokenStore.delete(resourceId);
//                    } else {
//                        throw new PermanentException(401, "Unauthorized", null);
//                    }
//                }
//            } catch (CoreTokenException e) {
//                throw new ServiceUnavailableException(e.getMessage(),e);
//            }
//            Map< String, String> responseVal = new HashMap< String, String>();
//            responseVal.put("success", "true");
//            response = new JsonValue(responseVal);
//            Resource resource = new Resource(resourceId, "1", response);
//            handler.handleResult(resource);
//        } catch (ResourceException e){
//            handler.handleError(e);
//        } catch (SSOException e){
//            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
//        } catch (IdRepoException e){
//            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
//        } catch (UnauthorizedClientException e) {
//            handler.handleError(new PermanentException(401, "Unauthorized", e));
//        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest queryRequest, QueryResultHandler handler){
//        try{
//            JsonValue response = null;
//            Resource resource;
//            try {
//                Map<String, Object> query = new HashMap<String, Object>();
//                String id = queryRequest.getQueryId();
//
//                //get uid of submitter
//                AMIdentity uid;
//                try {
//                    uid = getUid(context);
//                    if (!uid.equals(adminUserId)){
//                        query.put(OAuth2Constants.CoreTokenParams.USERNAME, uid.getName());
//                    } else {
//                        query.put(OAuth2Constants.CoreTokenParams.USERNAME, "*");
//                    }
//                } catch (Exception e){
//                    PermanentException ex = new PermanentException(401, "Unauthorized" ,e);
//                    handler.handleError(ex);
//                }
//
//                //split id into the query fields
//                String[] queries = id.split("\\,");
//                for (String q: queries){
//                    String[] params = q.split("=");
//                    if (params.length == 2){
//                        query.put(params[0], params[1]);
//                    }
//                }
//
//                response = tokenStore.query(query);
//            } catch (CoreTokenException e) {
//                throw new ServiceUnavailableException(e.getMessage(),e);
//            }
//            resource = new Resource("result", "1", response);
//            JsonValue value = resource.getContent();
//            Set<HashMap<String,Set<String>>> list = (Set<HashMap<String,Set<String>>>) value.getObject();
//            Resource res = null;
//            JsonValue val = null;
//            if (list != null && !list.isEmpty() ){
//                for (HashMap<String,Set<String>> entry : list){
//                    val = new JsonValue(entry);
//                    res = new Resource("result", "1", val);
//                    handler.handleResource(res);
//                }
//            }
//            handler.handleResult(new QueryResult());
//        } catch (ResourceException e){
//            handler.handleError(e);
//        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler){

//        AMIdentity uid = null;
//        String username = null;
//        try {
//            //first check if SSOToken is valid
//            uid = getUid(context);
//
//            JsonValue response;
//            Resource resource;
//            try {
//                response = tokenStore.read(resourceId);
//            } catch (CoreTokenException e) {
//                throw new NotFoundException("Token Not Found", e);
//            }
//            if (response == null){
//                throw new NotFoundException("Token Not Found", null);
//            }
//
//            Set<String> grantTypes = (Set<String>) response.get(OAuth2Constants.Params.GRANT_TYPE).getObject();
//            String grantType = null;
//            if (grantTypes != null && !grantTypes.isEmpty()){
//                grantType = grantTypes.iterator().next();
//            }
//
//            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
//                resource = new Resource(OAuth2Constants.Params.ID, "1", response);
//                handler.handleResult(resource);
//            } else {
//                Set<String> realms = (Set<String>) response.get(OAuth2Constants.CoreTokenParams.REALM).getObject();
//                String realm = null;
//                if (realms != null && !realms.isEmpty()){
//                    realm = realms.iterator().next();
//                }
//
//                Set<String> usernameSet = (Set<String>)response.get(OAuth2Constants.CoreTokenParams.USERNAME).getObject();
//                if (usernameSet != null && !usernameSet.isEmpty()){
//                    username = usernameSet.iterator().next();
//                }
//                if(username == null || username.isEmpty()){
//                    throw new PermanentException(404, "Not Found", null);
//                }
//                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
//                if (uid.equals(adminUserId) || uid.equals(uid2)){
//                    resource = new Resource(OAuth2Constants.Params.ID, "1", response);
//                    handler.handleResult(resource);
//                } else {
//                    throw new PermanentException(401, "Unauthorized" ,null);
//                }
//            }
//        } catch (ResourceException e){
//            handler.handleError(e);
//        } catch (SSOException e){
//            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
//        } catch (IdRepoException e){
//            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
//        } catch (UnauthorizedClientException e) {
//            handler.handleError(new PermanentException(401, "Unauthorized", e));
//        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

//    private String getCookieFromServerContext(ServerContext context) {
//        return RestUtils.getCookieFromServerContext(context);
//    }
//
//    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException, UnauthorizedClientException {
//        String cookie = getCookieFromServerContext(context);
//        SSOTokenManager mgr = SSOTokenManager.getInstance();
//        SSOToken token = mgr.createSSOToken(cookie);
//        return identityManager.getResourceOwnerIdentity(token.getProperty("UserToken"), token.getProperty("Organization"));
//    }
}
