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
import java.util.List;
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
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
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

        String searchForId;
        try {
            searchForId = request.getQueryFilter().accept(new AuthenticationModuleQueryFilterVisitor(), null);
        } catch (UnsupportedOperationException e) {
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
                if (searchForId == null || searchForId.equalsIgnoreCase(name)) {
                    ServiceSchemaManager schemaManager = getSchemaManager(instance.getType());
                    String type = schemaManager.getResourceName();
                    String typeDescription = getI18NValue(schemaManager, instance.getType(), debug);
                    JsonValue result = json(object(
                            field(Resource.FIELD_CONTENT_ID, name),
                            field("typeDescription", typeDescription),
                            field("type", type)));
                    handler.handleResource(new Resource(name, String.valueOf(result.hashCode()), result));
                }
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

    private ServiceSchemaManager getSchemaManager(String authType) throws SSOException, SMSException,
            AMConfigurationException {
        AMAuthenticationManager authenticationManager = new AMAuthenticationManager(adminToken, "/");
        AMAuthenticationSchema schema = authenticationManager.getAuthenticationSchema(authType);
        return new ServiceSchemaManager(schema.getServiceName(), adminToken);
    }

    static String getI18NValue(ServiceSchemaManager schemaManager, String authType, Debug debug) {
        String i18nKey = schemaManager.getI18NKey();
        String i18nName = authType;
        ResourceBundle rb = getBundle(schemaManager.getI18NFileName(), Locale.getDefaultLocale());
        if (rb != null && i18nKey != null && !i18nKey.isEmpty()) {
            i18nName = Locale.getString(rb, i18nKey, debug);
        }
        return i18nName;
    }

    private static ResourceBundle getBundle(String name, java.util.Locale locale) {
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

    private static final class AuthenticationModuleQueryFilterVisitor implements QueryFilterVisitor<String, Void> {

        @Override
        public String visitAndFilter(Void aVoid, List<QueryFilter> subFilters) {
            throw new UnsupportedOperationException("And is not supported");
        }

        @Override
        public String visitBooleanLiteralFilter(Void aVoid, boolean value) {
            if (value) {
                return null;
            } else {
                throw new UnsupportedOperationException("Boolean literal 'false' is not supported");
            }
        }

        @Override
        public String visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Contains is not supported");
        }

        @Override
        public String visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            if ("_id".equalsIgnoreCase(field.leaf())) {
                if (!(valueAssertion instanceof String)) {
                    throw new IllegalArgumentException("Invalid value assertion type: "
                            + valueAssertion.getClass().getSimpleName());
                }
                return (String) valueAssertion;
            }
            throw new UnsupportedOperationException("Equals is not supported");
        }

        @Override
        public String visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
            throw new UnsupportedOperationException("Extended match is not supported");
        }

        @Override
        public String visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Greater than is not supported");
        }

        @Override
        public String visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Greater than or equal to is not supported");
        }

        @Override
        public String visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Less than is not supported");
        }

        @Override
        public String visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Less than or equal to is not supported");
        }

        @Override
        public String visitNotFilter(Void aVoid, QueryFilter subFilter) {
            throw new UnsupportedOperationException("Not is not supported");
        }

        @Override
        public String visitOrFilter(Void aVoid, List<QueryFilter> subFilters) {
            throw new UnsupportedOperationException("Or is not supported");
        }

        @Override
        public String visitPresentFilter(Void aVoid, JsonPointer field) {
            throw new UnsupportedOperationException("Present is not supported");
        }

        @Override
        public String visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw new UnsupportedOperationException("Starts with is not supported");
        }
    }
}
