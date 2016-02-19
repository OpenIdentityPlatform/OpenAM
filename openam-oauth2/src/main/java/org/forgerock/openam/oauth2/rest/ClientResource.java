/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 ForgeRock AS.
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

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Named;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.oauth2.OAuth2AuditLogger;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.util.promise.Promise;

public class ClientResource implements CollectionResourceProvider {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final OAuth2AuditLogger auditLogger;
    private final ClientResourceManager manager;
    private final CTSPersistentStore store;
    private final Debug debug;
    private ServiceSchemaManager serviceSchemaManager = null;
    private ServiceSchema serviceSchema = null;

    @Inject
    public ClientResource(ClientResourceManager manager, CTSPersistentStore store, OAuth2AuditLogger auditLogger,
                          @Named("frRest") Debug debug) {
        this.store = store;
        this.manager = manager;
        this.auditLogger = auditLogger;
        this.debug = debug;
        try {
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            this.serviceSchemaManager = new ServiceSchemaManager("AgentService", token);
            this.serviceSchema = serviceSchemaManager.getOrganizationSchema().getSubSchema("OAuth2Client");
        } catch (Exception e){
            logger.error("Unable to get Client Schema", e);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", "Unable to get Client Schema"};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
        }
    }

    public ClientResource(ClientResourceManager manager, CTSPersistentStore store, ServiceSchemaManager mgr,
            OAuth2AuditLogger auditLogger, Debug debug) {
        this.store = store;
        this.manager = manager;
        this.auditLogger = auditLogger;
        this.debug = debug;
        try {
            this.serviceSchemaManager = mgr;
            this.serviceSchema = serviceSchemaManager.getOrganizationSchema().getSubSchema("OAuth2Client");
        } catch (Exception e){
            logger.error("Unable to get Client Schema", e);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", "Unable to get Client Schema"};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
        }
    }

    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest
            actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    public Promise<ResourceResponse, ResourceException> createInstance(Context context,
            CreateRequest createRequest) {

        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        Map< String, String> responseVal = new HashMap< String, String>();
        try {
            if (serviceSchema == null || serviceSchemaManager == null){
                if (debug.errorEnabled()) {
                    debug.error("ClientResource :: CREATE by " + principal + ": No serviceSchema available.");
                }
                throw new PermanentException(ResourceException.INTERNAL_ERROR, "", null);
            }

            Map<String, ArrayList<String>> client =
                    (Map<String, ArrayList<String>>) createRequest.getContent().getObject();
            String realm = null;
            if (client == null || client.isEmpty()){
                if (debug.errorEnabled()) {
                    debug.error("ClientResource :: CREATE by " + principal + ": No client definition.");
                }
                throw new PermanentException(ResourceException.BAD_REQUEST, "Missing client definition", null);
            }

            //check for id
            String id = createRequest.getNewResourceId();
            if (client.containsKey(OAuth2Constants.OAuth2Client.CLIENT_ID)){
                ArrayList<String> idList = client.remove(OAuth2Constants.OAuth2Client.CLIENT_ID);
                if (idList != null && !idList.isEmpty()){
                    id = idList.iterator().next();
                }
            }

            if (id == null || id.isEmpty()){
                debug.error("ClientResource :: CREATE by " + principal + ": No client ID.");
                throw new PermanentException(ResourceException.BAD_REQUEST, "Missing client id", null);
            }

            //get realm
            if (client.containsKey(OAuth2Constants.OAuth2Client.REALM)){
                ArrayList<String> realmList = client.remove(OAuth2Constants.OAuth2Client.REALM);
                if (realmList != null && !realmList.isEmpty()){
                    realm = realmList.iterator().next();
                }
            }

            //check for required parameters
            if (!client.containsKey(OAuth2Constants.OAuth2Client.USERPASSWORD) ||
                    client.get(OAuth2Constants.OAuth2Client.USERPASSWORD).iterator().next().isEmpty()){
                if (debug.errorEnabled()) {
                    debug.error("ClientResource :: CREATE by " + principal + ": " +
                            "Resource ID: " + id + ": No user password.");
                }
                throw new PermanentException(ResourceException.BAD_REQUEST, "Missing user password", null);
            }

            if (client.containsKey(OAuth2Constants.OAuth2Client.CLIENT_TYPE)) {
                String type = client.get(OAuth2Constants.OAuth2Client.CLIENT_TYPE).iterator().next();
                if (!(type.equals("Confidential") || type.equals("Public"))){
                    debug.error("ClientResource :: CREATE by " + principal + ": " +
                            "Resource ID: " + id + ": No client type.");
                    throw new PermanentException(ResourceException.BAD_REQUEST, "Missing client type", null);
                }
            } else {
                debug.error("ClientResource :: CREATE by" + principal + ": " +
                        "Resource ID: " + id + ": No client type.");
                throw new PermanentException(ResourceException.BAD_REQUEST, "Missing client type", null);
            }

            Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
            for (Map.Entry mapEntry : client.entrySet()){
                List<String> list = (ArrayList) mapEntry.getValue();
                Set<String> set = new HashSet<String>();
                if (isSingle((String) mapEntry.getKey())){
                    set.add((String)((ArrayList)mapEntry.getValue()).get(0));
                    } else {
                    for (int i = 0; i < list.size(); i++) {
                        set.add("[" + i + "]=" + list.get(i));
                    }
                }
                attrs.put((String)mapEntry.getKey(), set);
            }

            Set<String> temp = new HashSet<String>();
            temp.add("OAuth2Client");
            attrs.put("AgentType", temp);

            temp = new HashSet<String>();
            temp.add("Active");
            attrs.put("sunIdentityServerDeviceStatus", temp);

            manager.createIdentity(realm, id, attrs);
            responseVal.put("success", "true");

            JsonValue response = new JsonValue(responseVal);

            ResourceResponse resource = newResourceResponse("results", String.valueOf(currentTimeMillis()), response);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"CREATED_CLIENT", responseVal.toString()};
                auditLogger.logAccessMessage("CREATED_CLIENT", obs, null);
            }
            return newResultPromise(resource);
        } catch(IdRepoException e){
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }

            if (debug.errorEnabled()) {
                debug.error("ClientResource :: CREATE by " + principal + ": Unable to create client due to " +
                        "IdRepo exception.", e);
            }
            return new InternalServerErrorException("Unable to create client", e).asPromise();
        } catch (SSOException e){
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
            if (debug.errorEnabled()) {
                debug.error("ClientResource :: CREATE by " + principal + ": Unable to create client due to " +
                        "SSO exception.", e);
            }
            return new InternalServerErrorException("Unable to create client", e).asPromise();
        } catch (PermanentException e){
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
            if (debug.errorEnabled()) {
                debug.error("ClientResource :: CREATE by " + principal +
                        ": Unable to create client due to exception.", e);
            }
            return e.asPromise();
        } catch (org.forgerock.json.resource.BadRequestException e) {
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_CLIENT", obs, null);
            }
            debug.error("ClientResource :: CREATE : Unable to create client due to Bad Request.", e);
            return e.asPromise();
        }
    }

    private boolean isSingle(String value) throws org.forgerock.json.resource.BadRequestException {
        AttributeSchema attributeSchema = serviceSchema.getAttributeSchema(value);
        if (attributeSchema == null) {
            if (debug.errorEnabled()) {
                debug.error("ClientResource.isSingle() : Invalid OAuth2 Client attribute, " + value);
            }
            throw new org.forgerock.json.resource.BadRequestException("Invalid OAuth2 Client attribute, " + value);
        }
        AttributeSchema.UIType uiType = attributeSchema.getUIType();
        if (uiType != null && (uiType.equals(AttributeSchema.UIType.UNORDEREDLIST) ||
            uiType.equals(AttributeSchema.UIType.ORDEREDLIST))){
            return false;
        }
        return true;
    }

    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {

        String principal = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        Map<String, String> responseVal = new HashMap<String, String>();
        JsonValue response;
        try {
            String realm = request.getAdditionalParameter("realm");
            if (realm == null) {
                realm = "/";
            }
            manager.deleteIdentity(resourceId, realm);

            try {
                //delete the tokens associated with that client_id
                final TokenFilter tokenFilter = new TokenFilterBuilder().and()
                        .withAttribute(OAuthTokenField.CLIENT_ID.getField(), resourceId)
                        .withAttribute(OAuthTokenField.REALM.getField(), realm).build();
                store.deleteOnQueryAsync(tokenFilter);
            } catch (CoreTokenException e) {
                if (auditLogger.isAuditLogEnabled()) {
                    String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                    auditLogger.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
                }
                if (debug.errorEnabled()) {
                    debug.error("ClientResource :: DELETE by " + principal + ": Unable to delete client with ID, "
                            + resourceId);
                }
                throw new InternalServerErrorException("Unable to delete client", e);
            }

            responseVal.put("success", "true");
	        response = new JsonValue(responseVal);

            ResourceResponse resource = newResourceResponse("results", "1", response);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"DELETED_CLIENT", response.toString()};
                auditLogger.logAccessMessage("DELETED_CLIENT", obs, null);
                if (debug.messageEnabled()) {
                    debug.error("ClientResource :: DELETE by " + principal + ": delete client with ID, " + resourceId);
                }
            }
            return newResultPromise(resource);
        } catch(IdRepoException e) {
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            if (debug.errorEnabled()) {
                debug.error("ClientResource :: DELETE by " + principal +
                        ": Unable to delete client with ID, " + resourceId, e);
            }
            return new InternalServerErrorException("Unable to delete client", e).asPromise();
        } catch (SSOException e) {
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            if (debug.errorEnabled()) {
                debug.error("ClientResource :: DELETE by " + principal +
                        ": Unable to delete client with ID, " + resourceId, e);
            }
            return new InternalServerErrorException("Unable to delete client", e).asPromise();
        } catch (InternalServerErrorException e) {
            responseVal.put("success", "false");
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_DELETE_CLIENT", responseVal.toString()};
                auditLogger.logErrorMessage("FAILED_DELETE_CLIENT", obs, null);
            }
            if (debug.errorEnabled()) {
                debug.error("ClientResource :: DELETE by " + principal +
                        ": Unable to delete client with ID, " + resourceId, e);
            }
            return new InternalServerErrorException("Unable to delete client", e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest queryRequest,
            QueryResourceHandler handler) {
        return RestUtils.generateUnsupportedOperation();
    }

    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }
}
