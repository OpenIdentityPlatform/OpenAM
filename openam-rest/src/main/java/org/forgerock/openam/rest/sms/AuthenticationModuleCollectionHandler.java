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

package org.forgerock.openam.rest.sms;

import static org.forgerock.json.fluent.JsonValue.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.AccessController;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationSchema;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;

/**
 * Collection handler for handling queries on the {@literal /authentication/modules} resource.
 *
 * @since 13.0.0
 */
public class AuthenticationModuleCollectionHandler implements RequestHandler {

    private final Debug debug;
    private final SSOToken adminToken;

    @Inject
    AuthenticationModuleCollectionHandler(@Named("frRest") Debug debug) {
        this.debug = debug;
        this.adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Returns the list of configured authentication module instances for the current realm.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            handler.handleError(new NotSupportedException("Query not supported: " + request.getQueryFilter()));
            return;
        }
        if (request.getPagedResultsCookie() != null || request.getPagedResultsOffset() > 0 ||
                request.getPageSize() > 0) {
            handler.handleError(new NotSupportedException("Query paging not currently supported"));
            return;
        }

        try {
            SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            String realm = context.asContext(RealmContext.class).getResolvedRealm();
            AMAuthenticationManager mgr = new AMAuthenticationManager(ssoToken, realm);
            Set<AMAuthenticationInstance> moduleInstances = mgr.getAuthenticationInstances();

            for (AMAuthenticationInstance instance : moduleInstances) {
                String name = instance.getName();
                ServiceSchemaManager schemaManager = getSchemaManager(name);
                String typePath = schemaManager.getResourceName();
                String typeI18N = getI18NValue(schemaManager, instance.getType());
                JsonValue result = json(object(
                        field(Resource.FIELD_CONTENT_ID, name),
                        field("type", typeI18N),
                        field("path", typePath + "/" + name)));
                handler.handleResource(new Resource(name, String.valueOf(result.hashCode()), result));
            }

            handler.handleResult(new QueryResult());

        } catch (AMConfigurationException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: AMConfigurationException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SMSException e) {
            debug.warning("::AuthenticationModuleCollectionHandler:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    private ServiceSchemaManager getSchemaManager(String authName) throws SSOException, SMSException, AMConfigurationException {
        AMAuthenticationManager authenticationManager = new AMAuthenticationManager(adminToken/*userToken?*/, "/");
        AMAuthenticationSchema schema = authenticationManager.getAuthenticationSchema(authName);
        return new ServiceSchemaManager(schema.getServiceName(), adminToken);
    }

    private String getI18NValue(ServiceSchemaManager schemaManager, String authType) {
        String i18nKey = schemaManager.getI18NKey();
        String i18nName = authType;
        ResourceBundle rb = getBundle(schemaManager.getI18NFileName(), Locale.getDefaultLocale());
        if (rb != null && i18nKey != null && !i18nKey.isEmpty()) {
            i18nName = Locale.getString(rb, i18nKey, debug);
        }
        return i18nName;
    }

    private ResourceBundle getBundle(String name, java.util.Locale locale) {
        return AMResourceBundleCache.getInstance().getResBundle(name, locale);
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException(
                "The resource collection " + request.getResourceName() + " cannot perform actions"));
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException("Authentication modules must be created per type"));
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException(
                "The resource collection " + request.getResourceName() + " cannot be deleted"));
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException(
                "The resource collection " + request.getResourceName()  + " cannot be patched"));
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException("The resource collection " + request.getResourceName()
                + " cannot be read"));
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        // TODO: i18n
        handler.handleError(new BadRequestException(
                "The resource collection " + request.getResourceName() + " cannot be updated"));
    }
}
