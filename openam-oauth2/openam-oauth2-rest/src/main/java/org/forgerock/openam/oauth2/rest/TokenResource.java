/*
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
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
 * information: "Portions Copyrighted [2012] [ForgeRock Inc]".
 *
 */
package org.forgerock.openam.oauth2.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.ext.cts.repo.OpenDJTokenRepo;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;

import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.ext.cts.CoreTokenService;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.data.Status;

import java.security.AccessController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenResource implements CollectionResourceProvider {

    private JsonResource repository;

    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;
    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

    public TokenResource() {
        try {
            repository = new CoreTokenService(OpenDJTokenRepo.getInstance());
        } catch (Exception e) {
            throw new OAuthProblemException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getCode(),
                    "Service unavailable", "Could not create underlying storage", null);
        }
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
            uid = getUid(context);

            JsonValue query = new JsonValue(null);
            JsonValue response = null;
            Resource resource = null;
            JsonResourceAccessor accessor =
                    new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
            try {
                response = accessor.read(resourceId);
                Set<String> usernameSet = (Set<String>) response.get("username").getObject();
                if(usernameSet == null || usernameSet.isEmpty()){
                    PermanentException ex = new PermanentException(404, "Not Found", null);
                    handler.handleError(ex);
                }
                if (uid.getName().equalsIgnoreCase(usernameSet.iterator().next()) || uid.equals(adminUserId)){
                    response = accessor.delete(resourceId, "1");
                } else {
                    PermanentException ex = new PermanentException(401, "Unauthorized", null);
                    handler.handleError(ex);
                }
            } catch (JsonResourceException e) {
                throw new ServiceUnavailableException(e.getMessage(),e);
            }
            Map< String, String> responseVal = new HashMap< String, String>();
            responseVal.put("success", "true");
            response = new JsonValue(responseVal);
            resource = new Resource(resourceId, "1", response);
            handler.handleResult(resource);
        } catch (ResourceException e){
            handler.handleError(e);
        } catch (SSOException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (IdRepoException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
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
            JsonValue response;
            Resource resource;
            JsonResourceAccessor accessor =
                    new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
            try {
                Map query = new HashMap<String,String>();
                String id = queryRequest.getQueryId();

                //get uid of submitter
                AMIdentity uid = null;
                try {
                    uid = getUid(context);
                    if (!uid.equals(adminUserId)){
                        query.put("username", uid.getName());
                    } else {
                        query.put("username", "*");
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

                JsonValue queryFilter = new JsonValue(new HashMap<String, HashMap<String, String>>());
                if (query != null){
                    queryFilter.put("filter", query);
                }
                response = accessor.query("1", queryFilter);
            } catch (JsonResourceException e) {
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
            uid = getUid(context);

            JsonValue response;
            Resource resource;
            JsonResourceAccessor accessor =
                    new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
            try {
                response = accessor.read(resourceId);
                Set<String> usernameSet = (Set<String>) response.get("username").getObject();
                if(usernameSet == null || usernameSet.isEmpty()){
                    PermanentException ex = new PermanentException(404, "Not Found", null);
                    handler.handleError(ex);
                }
                username = usernameSet.iterator().next();

            } catch (JsonResourceException e) {
                throw new ServiceUnavailableException(e.getMessage(),e);
            }
            if (uid.equals(adminUserId) || username.equalsIgnoreCase(uid.getName())){
                resource = new Resource(OAuth2Constants.Params.ID, "1", response);
                handler.handleResult(resource);
            } else {
                throw new PermanentException(401, "Unauthorized" ,null);
            }
        } catch (ResourceException e){
            handler.handleError(e);
        } catch (SSOException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
        } catch (IdRepoException e){
            handler.handleError(new PermanentException(401, "Unauthorized" ,e));
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
        List<String> cookies = null;
        String cookieName = null;
        HttpContext header = null;
        try {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
            if (cookieName == null || cookieName.isEmpty()) {
                return null;
            }
            header = context.asContext(HttpContext.class);
            if (header == null) {
                return null;
            }
            //get the cookie from header directly   as the name of com.iplanet.am.cookie.am
            cookies = header.getHeaders().get(cookieName.toLowerCase());
            if (cookies != null && !cookies.isEmpty()) {
                for (String s : cookies) {
                    if (s == null || s.isEmpty()) {
                        return null;
                    } else {
                        return s;
                    }
                }
            } else {  //get cookie from header parameter called cookie
                cookies = header.getHeaders().get("cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    for (String cookie : cookies) {
                        String cookieNames[] = cookie.split(";"); //Split parameter up
                        for (String c : cookieNames) {
                            if (c.contains(cookieName)) { //if com.iplanet.am.cookie.name exists in cookie param
                                String amCookie = c.replace(cookieName + "=", "").trim();
                                return amCookie; //return com.iplanet.am.cookie.name value
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException{
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return IdUtils.getIdentity(token);
    }


}
