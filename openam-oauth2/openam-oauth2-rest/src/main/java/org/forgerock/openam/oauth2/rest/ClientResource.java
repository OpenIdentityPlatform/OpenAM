/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.coretoken.interfaces.OAuth2TokenRepository;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.ldap.CTSPersistentStore;
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

    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());

    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;
    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

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
        String realm = null;
        if (client == null || client.isEmpty()){
            ResourceException e =
                    new PermanentException(ResourceException.BAD_REQUEST, "Missing client definition", null);
            handler.handleError(e);
        }
        //check for id
        String id = createRequest.getNewResourceId();
        if (client.containsKey("client_id")){
            ArrayList<String> idList = client.remove("client_id");
            if (idList != null && !idList.isEmpty()){
                id = (String) idList.iterator().next();
            }
        }
        if (id == null || id.isEmpty()){
            ResourceException e =
                    new PermanentException(ResourceException.BAD_REQUEST, "Missing client id", null);
            handler.handleError(e);
        }

        //get realm
        if (client.containsKey("realm")){
            ArrayList<String> realmList = client.remove("realm");
            if (realmList != null && !realmList.isEmpty()){
                realm = (String) realmList.iterator().next();
            }
        }

        //check for required parameters
        if (client.containsKey("userpassword")){
            if (client.get("userpassword").iterator().next().isEmpty()){
                ResourceException e =
                        new PermanentException(ResourceException.BAD_REQUEST, "Missing userpassword", null);
                handler.handleError(e);
            }
        } else {
            ResourceException e =
                    new PermanentException(ResourceException.BAD_REQUEST, "Missing userpassword", null);
            handler.handleError(e);
        }
        if (client.containsKey("com.forgerock.openam.oauth2provider.clientType")){
            String type = client.get("com.forgerock.openam.oauth2provider.clientType").iterator().next();
            if (type.equals("Confidential") || type.equals("Public")){
                //do nothing
            } else {
                ResourceException e =
                        new PermanentException(ResourceException.BAD_REQUEST, "Missing client type", null);
                handler.handleError(e);
            }
        } else {
            ResourceException e =
                    new PermanentException(ResourceException.BAD_REQUEST, "Missing client type", null);
            handler.handleError(e);
        }

        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        for (Map.Entry mapEntry : client.entrySet()){
            attrs.put((String)mapEntry.getKey(), new HashSet<String>((ArrayList)mapEntry.getValue()));
        }

        Set<String> temp = new HashSet<String>();
        temp.add("OAuth2Client");
        attrs.put("AgentType", temp);

        temp = new HashSet<String>();
        temp.add("Active");
        attrs.put("sunIdentityServerDeviceStatus", temp);

        JsonValue response = null;
        Map< String, String> responseVal =new HashMap< String, String>();
        String uid;
        try {
            if (!(getUid(context).equals(adminUserId))){
                throw new PermanentException(401, "Unauthorized", null);
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
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (SSOException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to create client"));
        } catch (PermanentException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
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
            if (!(getUid(context).equals(adminUserId))){
                throw new PermanentException(401, "Unauthorized", null);
            }
            //Delete the client_id
            AMIdentityRepository repo = new AMIdentityRepository(token , null);
            Set<AMIdentity> ids = new HashSet<AMIdentity>();
            ids.add(getIdentity(resourceId, null));
            repo.deleteIdentities(ids);

            //delete the tokens associated with that client_id
            OAuth2TokenRepository tokens = CTSPersistentStore.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(OAuth2Constants.Params.CLIENTID).append("=").append(resourceId).append(")");
            try {
                tokens.oauth2DeleteWithFilter(sb.toString());
            } catch (Exception e){
                if (OAuth2Utils.logStatus) {
                    String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                    OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
                }
                handler.handleError(new InternalServerErrorException("Unable to delete client"));
            }
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
            handler.handleError(new InternalServerErrorException("Unable to delete client"));
        } catch (SSOException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to delete client"));
        } catch (InternalServerErrorException e){
            responseVal.put("success", "false");
            if (OAuth2Utils.logStatus) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                OAuth2Utils.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            handler.handleError(new InternalServerErrorException("Unable to delete client"));
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

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException{
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return IdUtils.getIdentity(token);

    }
}
