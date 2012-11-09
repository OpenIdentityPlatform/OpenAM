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
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;

import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.*;

import java.security.AccessController;
import java.util.*;

public class ClientResource  implements CollectionResourceProvider {

    SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());

    public ClientResource() {
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

        Map<String, ArrayList<String>> client = (Map<String, ArrayList<String>>) createRequest.getContent().getObject();
        String realm = client.remove("realm").iterator().next();
        String id = client.remove("id").iterator().next();

        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        for (Map.Entry mapEntry : client.entrySet()){
            attrs.put((String)mapEntry.getKey(), new HashSet<String>((ArrayList)mapEntry.getValue()));
        }

        JsonValue response = null;
        Map< String, String> responseVal =new HashMap< String, String>();
        String uid;
        try {
            uid = getUid(context);
            if (!uid.equals("amadmin")){
                throw new PermanentException(402, "Unauthorized", null);
            }
            AMIdentityRepository repo = new AMIdentityRepository(token , realm);
            repo.createIdentity(IdType.AGENTONLY, id, attrs);
            responseVal.put("success", "true");

            response = new JsonValue(responseVal);

            Resource resource = new Resource("results", "1", response);
            if (OAuth2Utils.logStatus) {
                String[] obs = {"CREATED_CLIENT", responseVal.toString()};
                OAuth2Utils.logAccessMessage("CREATED_CLIENT", obs, null);
            }
            handler.handleResult(resource);
        } catch(IdRepoException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (SSOException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (PermanentException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(e);
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler){
        Map<String, String> responseVal = new HashMap<String, String>();
        JsonValue response = null;
        String uid;
        try {
            uid = getUid(context);
            if (!uid.equals("amadmin")){
                throw new PermanentException(402, "Unauthorized", null);
            }
            AMIdentityRepository repo = new AMIdentityRepository(token , null);
            Set<AMIdentity> ids = new HashSet<AMIdentity>();
            ids.add(getIdentity(resourceId, null));
            repo.deleteIdentities(ids);
            responseVal.put("success", "true");

            response = new JsonValue(responseVal);

            Resource resource = new Resource("results", "1", response);
            if (OAuth2Utils.logStatus) {
                String[] obs = {"DELETED_CLIENT", response.toString()};
                OAuth2Utils.logAccessMessage("DELETED_CLIENT", obs, null);
            }
            handler.handleResult(resource);
        } catch(IdRepoException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (SSOException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (InternalServerErrorException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (PermanentException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(e);
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
        final ResourceException e =
                new NotSupportedException("Query is not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Read is not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Update is not supported for resource instances");
        handler.handleError(e);
    }

    private AMIdentity getIdentity(String uName, String realm) throws InternalServerErrorException {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = null;
        try{
            amIdRepo = new AMIdentityRepository(token , realm);
        } catch (IdRepoException e){
            throw new InternalServerErrorException("Unable to get idrepo", e);
        } catch (SSOException e){
            throw new InternalServerErrorException("Unable to get idrepo", e);
        }
        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set<AMIdentity> results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENTONLY, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw new InternalServerErrorException("Too many results or not enough");
            }

            theID = results.iterator().next();
        } catch (IdRepoException e) {
            throw new InternalServerErrorException("Unable to get search results", e);
        } catch (SSOException e) {
            throw new InternalServerErrorException("Unable to get search results", e);
        }
        return theID;
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

    private String getUid(ServerContext context) throws SSOException, IdRepoException{
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        AMIdentity id = IdUtils.getIdentity(token);
        return id.getName();
    }
}
