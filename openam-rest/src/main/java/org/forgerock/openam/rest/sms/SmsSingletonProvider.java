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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
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
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * A CREST singleton provider for SMS schema config.
 * @since 13.0.0
 */
public class SmsSingletonProvider extends SmsResourceProvider implements RequestHandler {

    final ServiceSchema dynamicSchema;
    private final SmsJsonConverter dynamicConverter;

    @Inject
    SmsSingletonProvider(@Assisted SmsJsonConverter converter,  @Assisted("schema") ServiceSchema schema,
            @Assisted("dynamic") @Nullable ServiceSchema dynamicSchema, @Assisted SchemaType type,
            @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
        this.dynamicSchema = dynamicSchema;
        if (dynamicSchema != null) {
            this.dynamicConverter = new SmsJsonConverter(dynamicSchema);
        } else {
            this.dynamicConverter = null;
        }
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
            JsonValue result = withExtraAttributes(serverContext, convertToJson(config));
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

    protected Map<String, Set<String>> getDynamicAttributes(ServerContext context) {
        return AuthD.getAuth().getOrgServiceAttributes(realmFor(context), serviceName);
    }

    /**
     * Augments the provided {@code JsonValue} to include any dynamic attributes, if present.
     *
     * @param value The {@code JsonValue} to augment.
     * @return The same {@code JsonValue} after is has been augmented.
     */
    protected JsonValue withExtraAttributes(ServerContext context, JsonValue value) {
        if (dynamicConverter != null) {
            value.add("dynamic", dynamicConverter.toJson(getDynamicAttributes(context)).getObject());
        }
        return value;
    }

    private void updateDynamicAttributes(ServerContext context, JsonValue value) throws SMSException, SSOException,
            IdRepoException {
        Map<String, Set<String>> dynamic = dynamicConverter.fromJson(value.get("dynamic"));
        if (SchemaType.GLOBAL.equals(type)) {
            dynamicSchema.setAttributeDefaults(dynamic);
        } else {
            AuthD.getAuth().setOrgServiceAttributes(realmFor(context), serviceName, dynamic);
        }
    }

    /**
     * Updates config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext serverContext, UpdateRequest updateRequest, ResultHandler<Resource> handler) {
        String resourceId = resourceId();
        if (dynamicSchema != null) {
            try {
                updateDynamicAttributes(serverContext, updateRequest.getContent());
            } catch (SMSException e) {
                debug.warning("::SmsCollectionProvider:: SMSException on create", e);
                handler.handleError(new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()));
            } catch (SSOException e) {
                debug.warning("::SmsCollectionProvider:: SSOException on create", e);
                handler.handleError(new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()));
            } catch (IdRepoException e) {
                debug.warning("::SmsCollectionProvider:: IdRepoException on create", e);
                handler.handleError(new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()));
            }
        }
        try {
            ServiceConfig config = getServiceConfigNode(serverContext, resourceId);
            saveConfigAttributes(config, convertFromJson(updateRequest.getContent()));
            JsonValue result = withExtraAttributes(serverContext, convertToJson(config));
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
        Map<String, Set<String>> attrs = convertFromJson(createRequest.getContent());
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
            JsonValue result = withExtraAttributes(serverContext, convertToJson(config));
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

    @Override
    protected JsonValue createTemplate(ServerContext context) {
        JsonValue result = super.createTemplate(context);
        if (dynamicSchema != null) {
            Map<String, String> attributeSectionMap = getAttributeNameToSection(dynamicSchema);
            ResourceBundle console = ResourceBundle.getBundle("amConsole");
            String serviceType = dynamicSchema.getServiceType().getType();
            String sectionOrder = getConsoleString(console, "sections." + serviceName + "." + serviceType);
            List<String> sections = new ArrayList<String>();
            if (StringUtils.isNotEmpty(sectionOrder)) {
                sections.addAll(Arrays.asList(sectionOrder.split("\\s+")));
            }
            addAttributeSchema(result, "/_schema/properties/dynamic/", dynamicSchema, sections, attributeSectionMap,
                    console, serviceType, context);
        }
        return result;
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
    protected ServiceConfig getServiceConfigNode(ServerContext serverContext, String resourceId) throws SSOException,
            SMSException, NotFoundException {
        ServiceConfigManager scm = getServiceConfigManager(serverContext);
        ServiceConfig result;
        if (subSchemaPath.isEmpty()) {
            if (type == SchemaType.GLOBAL) {
                result = getGlobalConfigNode(scm, resourceId);
            } else {
                result = scm.getOrganizationConfig(realmFor(serverContext), resourceId);
                if ((result == null || !result.exists()) && dynamicSchema == null) {
                    throw new NotFoundException();
                }
            }
        } else {
            ServiceConfig config = parentSubConfigFor(serverContext, scm);
            result = checkedInstanceSubConfig(resourceId, config);
            if (result == null || !result.exists()) {
                throw new NotFoundException();
            }
        }
        return result;
    }

    /**
     * Gets the referenced global {@link ServiceConfig} for the current request.
     *
     * @param scm The {@code ServerConfigManager} instance.
     * @param resourceId The name of the config. If this is root Schema config, this will be null. Otherwise, it will
     *                   be the name of the schema type.
     * @return The global instance retrieved from the service manager layer.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     * @throws NotFoundException If the config being addressed doesn't exist.
     */
    protected ServiceConfig getGlobalConfigNode(ServiceConfigManager scm, String resourceId) throws SSOException,
            SMSException, NotFoundException {
        ServiceConfig result = scm.getGlobalConfig(resourceId);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    private JsonValue convertToJson(ServiceConfig config) {
        if (config == null) {
            return json(object());
        } else {
            return converter.toJson(config.getAttributes());
        }
    }

    protected JsonValue preprocessJsonValue(JsonValue value) {
        value.remove("defaults");
        value.remove("dynamic");
        return value;
    }

    private Map<String, Set<String>> convertFromJson(JsonValue value) {
        preprocessJsonValue(value);
        return converter.fromJson(value);
    }

    protected void saveConfigAttributes(ServiceConfig config, Map<String, Set<String>> attributes) throws SSOException,
            SMSException {
        if (config != null) {
            config.setAttributes(attributes);
        }
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
