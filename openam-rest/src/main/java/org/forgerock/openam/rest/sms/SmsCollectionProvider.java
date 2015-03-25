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
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
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
 * A CREST collection provider for SMS schema config.
 */
public class SmsCollectionProvider extends SmsResourceProvider implements CollectionResourceProvider {

    @Inject
    SmsCollectionProvider(@Assisted SmsJsonConverter converter, @Assisted ServiceSchema schema,
            @Assisted SchemaType type, @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
        Reject.ifTrue(subSchemaPath.isEmpty(), "Root schemas do not support multiple instances");
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handleAction(request, handler);
    }

    /**
     * Creates a new child instance of config. The parent config referenced by the request path is found, and
     * new config is created using the provided name property.
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        Map<String, Object> attrs = converter.fromJson(content);
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            String name = content.get("_id").asString();
            if (name == null) {
                name = request.getNewResourceId();
            } else if (!name.equals(request.getNewResourceId())) {
                handler.handleError(ResourceException.getException(ResourceException.BAD_REQUEST,
                        "name and URI's resource ID do not match"));
            }
            config.addSubConfig(name, lastSchemaNodeName(), 0, attrs);
            ServiceConfig created = checkedInstanceSubConfig(name, config);

            String dn = created.getDN();
            JsonValue result = getJsonValue(created);
            handler.handleResult(new Resource(dn.substring(dn.lastIndexOf("=") + 1), String.valueOf(result.hashCode()),
                    result));
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
     * Deletes a child instance of config. The parent config referenced by the request path is found, and
     * the config is deleted using the resourceId.
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            checkedInstanceSubConfig(resourceId, config);
            config.removeSubConfig(resourceId);

            Resource resource = new Resource(resourceId, "0", json(object(field("success", true))));
            handler.handleResult(resource);
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
     * Reads a child instance of config. The parent config referenced by the request path is found, and
     * the config is read using the resourceId.
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            ServiceConfig item = checkedInstanceSubConfig(resourceId, config);

            JsonValue result = getJsonValue(item);
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
     * Updates a child instance of config. The parent config referenced by the request path is found, and
     * the config is updated using the resourceId.
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        Map<String, Object> attrs = converter.fromJson(content);
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            ServiceConfig node = checkedInstanceSubConfig(resourceId, config);

            node.setAttributes(attrs);
            JsonValue result = getJsonValue(node);
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
     * Queries for child instances of config. The parent config referenced by the request path is found, and
     * all child config for the type is returned.
     * <p>
     * Note that only query filter is supported, and only a filter of value {@code true} (i.e. all values).
     * Sorting and paging are not supported.
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
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
            ServiceConfigManager scm = getServiceConfigManager(context);
            if (subSchemaPath.isEmpty()) {
                Set<String> instanceNames = new TreeSet<String>(scm.getInstanceNames());
                String realm = null;
                if (type == SchemaType.ORGANIZATION) {
                    realm = realmFor(context);
                }
                for (String instanceName : instanceNames) {
                    ServiceConfig config = type == SchemaType.GLOBAL ? scm.getGlobalConfig(instanceName) :
                            scm.getOrganizationConfig(realm, instanceName);
                    if (config != null) {
                        JsonValue value = getJsonValue(config);
                        handler.handleResource(new Resource(instanceName, String.valueOf(value.hashCode()), value));
                    }
                }
            } else {
                ServiceConfig config = parentSubConfigFor(context, scm);
                Set<String> names = config.getSubConfigNames("*", lastSchemaNodeName());
                for (String configName : names) {
                    JsonValue value = getJsonValue(config.getSubConfig(configName));
                    handler.handleResource(new Resource(configName, String.valueOf(value.hashCode()), value));
                }
            }

            handler.handleResult(new QueryResult());
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    /**
     * Returns the JsonValue representation of the ServiceConfig using the {@link #converter}. Adds a {@code _id}
     * property for the name of the config.
     */
    private JsonValue getJsonValue(ServiceConfig result) {
        JsonValue value = converter.toJson(result.getAttributes());
        value.add("_id", result.getName());
        return value;
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException(request.getAction() + " action not supported"));
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("patch operation not supported"));
    }

}
