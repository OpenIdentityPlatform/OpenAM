/**
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.rest;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.OAuth2Constants;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import com.sun.identity.sm.ldap.api.fields.CoreTokenField;
import com.sun.identity.sm.ldap.api.fields.OAuthTokenField;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientResource  implements CollectionResourceProvider {

    private ClientResourceManager manager;
    private CTSPersistentStore store;

    public ClientResource() {
        this.store = CTSPersistentStore.getInstance();
        this.manager = new ClientResourceManager();
    }

    public ClientResource(ClientResourceManager manager, CTSPersistentStore store) {
        this.store = store;
        this.manager = manager;
    }

    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler){
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler){
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

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
        if (client.containsKey(OAuth2Constants.OAuth2Client.CLIENT_ID)){
            ArrayList<String> idList = client.remove(OAuth2Constants.OAuth2Client.CLIENT_ID);
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
        if (client.containsKey(OAuth2Constants.OAuth2Client.REALM)){
            ArrayList<String> realmList = client.remove(OAuth2Constants.OAuth2Client.REALM);
            if (realmList != null && !realmList.isEmpty()){
                realm = (String) realmList.iterator().next();
            }
        }

        //check for required parameters
        if (client.containsKey(OAuth2Constants.OAuth2Client.USERPASSWORD)){
            if (client.get(OAuth2Constants.OAuth2Client.USERPASSWORD).iterator().next().isEmpty()){
                ResourceException e =
                        new PermanentException(ResourceException.BAD_REQUEST, "Missing userpassword", null);
                handler.handleError(e);
            }
        } else {
            ResourceException e =
                    new PermanentException(ResourceException.BAD_REQUEST, "Missing userpassword", null);
            handler.handleError(e);
        }
        if (client.containsKey(OAuth2Constants.OAuth2Client.CLIENT_TYPE)){
            String type = client.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE).iterator().next();
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
            if (!manager.usersEqual(context)){
                throw new PermanentException(401, "Unauthorized", null);
            }
            manager.createIdentity(realm, id, attrs);
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

    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler){
        Map<String, String> responseVal = new HashMap<String, String>();
        JsonValue response = null;
        String uid;
        try {
            if (!manager.usersEqual(context)){
                throw new PermanentException(401, "Unauthorized", null);
            }
            manager.deleteIdentity(resourceId);

            //delete the tokens associated with that client_id
            Map<CoreTokenField, Object> query = new HashMap<CoreTokenField, Object>();
            query.put(OAuthTokenField.CLIENT_ID.getField(), resourceId);
            try {
                store.delete(query);
            } catch (DeleteFailedException e) {
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

    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Patch is not supported for resource instances");
        handler.handleError(e);
    }

    public void queryCollection(ServerContext context, QueryRequest queryRequest, QueryResultHandler handler){
        final ResourceException e =
                new NotSupportedException("Query is not supported for resource instances");
        handler.handleError(e);
    }

    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Read is not supported for resource instances");
        handler.handleError(e);
    }

    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler){
        final ResourceException e =
                new NotSupportedException("Update is not supported for resource instances");
        handler.handleError(e);
    }
}
