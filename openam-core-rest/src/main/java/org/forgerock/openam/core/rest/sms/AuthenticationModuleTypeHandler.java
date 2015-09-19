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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.core.rest.sms.AuthenticationModuleCollectionHandler.getI18NValue;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.AccessController;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;

/**
 * Collection handler for handling queries on the {@literal /authentication/modules/types} resource.
 *
 * @since 13.0.0
 */
public class AuthenticationModuleTypeHandler implements RequestHandler {

    private final Debug debug;
    private final SSOToken adminToken;

    @Inject
    AuthenticationModuleTypeHandler(@Named("frRest") Debug debug) {
        this.debug = debug;
        this.adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Returns the list of configured authentication module instances for the current realm.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new NotSupportedException("Query not supported: " + request.getQueryFilter()).asPromise();
        }
        if (request.getPagedResultsCookie() != null || request.getPagedResultsOffset() > 0 ||
                request.getPageSize() > 0) {
            return new NotSupportedException("Query paging not currently supported").asPromise();
        }

        try {
            SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            String realm = context.asContext(RealmContext.class).getResolvedRealm();
            AMAuthenticationManager mgr = new AMAuthenticationManager(ssoToken, realm);
            Set<String> authenticationServiceNames = AMAuthenticationManager.getAuthenticationServiceNames();

            for (String serviceName : authenticationServiceNames) {
                ServiceSchemaManager schemaManager = new ServiceSchemaManager(serviceName, adminToken);


                String resourceId = schemaManager.getResourceName();
                String typeI18N = getI18NValue(schemaManager, resourceId, debug);
                JsonValue result = json(object(
                        field(ResourceResponse.FIELD_CONTENT_ID, resourceId),
                        field("name", typeI18N)));
                handler.handleResource(newResourceResponse(resourceId, String.valueOf(result.hashCode()), result));
            }

            return newResultPromise(newQueryResponse());

        } catch (AMConfigurationException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: AMConfigurationException on create", e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: SSOException on create", e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        } catch (SMSException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: SMSException on create", e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        // TODO: i18n
        return new BadRequestException("The resource collection " + request.getResourcePath()
                + " cannot perform actions").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        // TODO: i18n
        return new BadRequestException("Authentication modules must be created per type").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        // TODO: i18n
        return new BadRequestException("The resource collection " + request.getResourcePath() + " cannot be deleted")
                .asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        // TODO: i18n
        return new BadRequestException("The resource collection " + request.getResourcePath() + " cannot be patched")
                .asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        // TODO: i18n
        return new BadRequestException("The resource collection " + request.getResourcePath() + " cannot be read")
                .asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        // TODO: i18n
        return new BadRequestException("The resource collection " + request.getResourcePath() + " cannot be updated")
                .asPromise();
    }
}
