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

import java.util.Arrays;
import java.util.Collections;
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
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
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

    private final List<ServiceSchema> subSchemaPath;
    private final SchemaType type;
    private final boolean hasInstanceName;
    private final SmsJsonConverter converter;
    private final Debug debug;
    private final List<String> uriPath;

    @Inject
    SmsCollectionProvider(@Assisted SmsJsonConverter converter, @Assisted ServiceSchema schema,
            @Assisted SchemaType type, @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug) {
        super(schema);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
        this.converter = converter;
        this.type = type;
        this.subSchemaPath = subSchemaPath;
        this.uriPath = uriPath == null ? Collections.<String>emptyList() : Arrays.asList(uriPath.split("/"));
        this.hasInstanceName = serviceHasInstanceName;
        this.debug = debug;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        // Template resource action will go here.
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        Map<String, Object> attrs = converter.fromJson(content);
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig result;
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    result = scm.createGlobalConfig(attrs);
                } else {
                    result = scm.createOrganizationConfig(realmFor(context), attrs);
                }
            } else {
                ServiceConfig config = parentSubSchemaFor(context, scm);
                String name = content.get("name").asString();
                config.addSubConfig(name, lastSchemaNodeName(), 0, attrs);
                result = config.getSubConfig(name);
            }

            String dn = result.getDN();
            handler.handleResult(new Resource(dn.substring(dn.lastIndexOf("=") + 1), String.valueOf(result.hashCode()),
                    converter.toJson(result.getAttributes())));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    scm.removeGlobalConfiguration(resourceId);
                } else {
                    scm.removeOrganizationConfiguration(realmFor(context), resourceId);
                }
            } else {
                ServiceConfig config = parentSubSchemaFor(context, scm);
                config.removeSubConfig(resourceId);
            }

            Resource resource = new Resource(resourceId, "0", json(object(field("success", true))));
            handler.handleResult(resource);
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig result;
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    result = scm.getGlobalConfig(resourceId);
                } else {
                    result = scm.getOrganizationConfig(realmFor(context), resourceId);
                }
            } else {
                ServiceConfig config = parentSubSchemaFor(context, scm);
                result = config.getSubConfig(resourceId);
            }

            handler.handleResult(new Resource(resourceId, String.valueOf(result.hashCode()),
                    converter.toJson(result.getAttributes())));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        JsonValue content = request.getContent();
        Map<String, Object> attrs = converter.fromJson(content);
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig node;
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    node = scm.getGlobalConfig(resourceId);
                } else {
                    node = scm.getOrganizationConfig(realmFor(context), resourceId);
                }
            } else {
                ServiceConfig config = parentSubSchemaFor(context, scm);
                node = config.getSubConfig(resourceId);
            }

            node.setAttributes(attrs);
            Map<String, Object> result = node.getAttributes();

            handler.handleResult(new Resource(resourceId, String.valueOf(result.hashCode()), converter.toJson(result)));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            handler.handleError(new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()));
        }
    }

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
                        Map<String, Object> result = config.getAttributes();
                        handler.handleResource(new Resource(instanceName, String.valueOf(result.hashCode()),
                                converter.toJson(result)));
                    }
                }
            } else {
                ServiceConfig config = parentSubSchemaFor(context, scm);
                Set<String> names = config.getSubConfigNames("*", lastSchemaNodeName());
                for (String configName : names) {
                    Map<String, Object> result = config.getSubConfig(configName).getAttributes();
                    handler.handleResource(new Resource(configName, String.valueOf(result.hashCode()),
                            converter.toJson(result)));
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

    protected String lastSchemaNodeName() {
        return subSchemaPath.get(subSchemaPath.size() - 1).getName();
    }

    protected ServiceConfig parentSubSchemaFor(ServerContext context, ServiceConfigManager scm)
            throws SMSException, SSOException {
        String name = null;
        Map<String, String> uriTemplateVariables = context.asContext(RouterContext.class).getUriTemplateVariables();
        if (hasInstanceName) {
            name = uriTemplateVariables.get("name");
        }
        ServiceConfig config = type == SchemaType.GLOBAL ?
                scm.getGlobalConfig(name) : scm.getOrganizationConfig(realmFor(context), name);
        for (int i = 0; i < subSchemaPath.size() - 1; i++) {
            ServiceSchema schema = subSchemaPath.get(i);
            String pathFragment = schema.getResourceName();
            if (pathFragment == null) {
                pathFragment = schema.getName();
            }
            if (uriPath.contains("{" + pathFragment + "}")) {
                pathFragment = uriTemplateVariables.get(pathFragment);
            }
            config = config.getSubConfig(pathFragment);
        }
        return config;
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
