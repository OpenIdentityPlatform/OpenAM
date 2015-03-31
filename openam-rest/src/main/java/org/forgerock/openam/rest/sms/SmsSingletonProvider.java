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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

/**
 * A CREST singleton provider for SMS schema config.
 * @since 13.0.0
 */
public class SmsSingletonProvider extends SmsResourceProvider implements RequestHandler {

    @Inject
    SmsSingletonProvider(@Assisted SmsJsonConverter converter, @Assisted ServiceSchema schema,
            @Assisted SchemaType type, @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
    }

    /**
     * Reads config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext serverContext, ReadRequest readRequest, ResultHandler<Resource> handler) {
        String resourceId = resourceId();
        try {
            ServiceConfig config = getServiceConfigNode(serverContext, resourceId);
            JsonValue result = converter.toJson(config.getAttributes());
            handler.handleResult(new Resource(resourceId, String.valueOf(result.hashCode()), result));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (NotFoundException e) {
            handler.handleError(e);
        }
    }

    /**
     * Updates config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext serverContext, UpdateRequest updateRequest, ResultHandler<Resource> handler) {
        String resourceId = resourceId();
        try {
            ServiceConfig config = getServiceConfigNode(serverContext, resourceId);
            config.setAttributes(converter.fromJson(updateRequest.getContent()));
            JsonValue result = converter.toJson(config.getAttributes());
            handler.handleResult(new Resource(resourceId, String.valueOf(result.hashCode()), result));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (NotFoundException e) {
            handler.handleError(e);
        }
    }

    /**
     * Deletes config for the singleton instance referenced.
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext serverContext, DeleteRequest deleteRequest, ResultHandler<Resource> handler) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    scm.removeGlobalConfiguration(null);
                } else {
                    scm.deleteOrganizationConfig(realmFor(serverContext));
                }
            } else {
                ServiceConfig parent = parentSubConfigFor(serverContext, scm);
                parent.removeSubConfig(resourceId());
            }
            handler.handleResult(new Resource(resourceId(), "0", json(object(field("success", true)))));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    /**
     * Creates config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext serverContext, CreateRequest createRequest, ResultHandler<Resource> handler) {
        Map<String, Set<String>> attrs = converter.fromJson(createRequest.getContent());
        try {
            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            ServiceConfig config;
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    config = scm.createGlobalConfig(attrs);
                } else {
                    config = scm.createOrganizationConfig(realmFor(serverContext), attrs);
                }
            } else {
                ServiceConfig parent = parentSubConfigFor(serverContext, scm);
                parent.addSubConfig(resourceId(), lastSchemaNodeName(), -1, attrs);
                config = parent.getSubConfig(lastSchemaNodeName());
            }
            JsonValue result = converter.toJson(config.getAttributes());
            handler.handleResult(new Resource(resourceId(), String.valueOf(result.hashCode()), result));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        super.handleAction(context, request, handler);
    }

    /**
     * Gets the referenced {@link ServiceConfig} for the current request.
     * @param serverContext The request context.
     * @param resourceId The name of the config. If this is root Schema config, this will be null. Otherwise, it will
     *                   be the name of the schema type.
     * @return The instance retrieved from the service manager layer.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     * @throws NotFoundException If the config being addressed doesn't exist.
     */
    protected ServiceConfig getServiceConfigNode(ServerContext serverContext, String resourceId) throws SSOException, SMSException, NotFoundException {
        ServiceConfigManager scm = getServiceConfigManager(serverContext);
        ServiceConfig result;
        if (subSchemaPath.isEmpty()) {
            if (type == SchemaType.GLOBAL) {
                result = scm.getGlobalConfig(resourceId);
            } else {
                result = scm.getOrganizationConfig(realmFor(serverContext), resourceId);
            }
        } else {
            ServiceConfig config = parentSubConfigFor(serverContext, scm);
            result = checkedInstanceSubConfig(resourceId, config);
        }
        if (result == null || !result.exists()) {
            throw new NotFoundException();
        }
        return result;
    }

    /**
     * Gets the resource ID. For root Schema config, this will be null. Otherwise, it will be the name of the schema
     * type this provider addresses.
     */
    private String resourceId() {
        return subSchemaPath.isEmpty() ? null : lastSchemaNodeName();
    }

    @Override
    public void handleQuery(ServerContext serverContext, QueryRequest queryRequest, QueryResultHandler handler) {
        handler.handleError(new NotSupportedException("query operation not supported"));
    }

    @Override
    public void handlePatch(ServerContext serverContext, PatchRequest patchRequest, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("patch operation not supported"));
    }

}
