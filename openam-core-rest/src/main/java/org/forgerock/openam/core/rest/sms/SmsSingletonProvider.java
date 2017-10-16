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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.models.Create.create;
import static org.forgerock.api.models.Delete.delete;
import static org.forgerock.api.models.Read.read;
import static org.forgerock.api.models.Update.update;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.VersionedPath;
import com.google.common.base.Optional;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

/**
 * A CREST singleton provider for SMS schema config.
 * @since 13.0.0
 */
@RequestHandler(@Handler(mvccSupported = false, resourceSchema = @Schema(fromType = Object.class)))
public class SmsSingletonProvider extends SmsResourceProvider {

    final ServiceSchema dynamicSchema;
    private final SmsJsonConverter dynamicConverter;
    private AMIdentityRepositoryFactory idRepoFactory;
    private ApiDescription description;

    @Inject
    SmsSingletonProvider(@Assisted SmsJsonConverter converter,  @Assisted("schema") ServiceSchema schema,
            @Assisted("dynamic") @Nullable ServiceSchema dynamicSchema, @Assisted SchemaType type,
            @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug,
            @Named("AMResourceBundleCache") AMResourceBundleCache resourceBundleCache,
            @Named("DefaultLocale") Locale defaultLocale, AMIdentityRepositoryFactory idRepoFactory) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug, resourceBundleCache,
                defaultLocale);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
        this.dynamicSchema = dynamicSchema;
        if (dynamicSchema != null) {
            this.dynamicConverter = new SmsJsonConverter(dynamicSchema);
        } else {
            this.dynamicConverter = null;
        }
        this.idRepoFactory = idRepoFactory;

        description = initDescription(schema);
    }

    protected ApiDescription initDescription(ServiceSchema schema) {
        return ApiDescription.apiDescription().id("fake").version("v")
                .paths(Paths.paths().put("", VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, Resource.resource()
                                .title(getI18NName())
                                .description(getSchemaDescription(schema.getI18NKey()))
                                .mvccSupported(false)
                                .resourceSchema(org.forgerock.api.models.Schema.schema().schema(
                                        createSchema(Optional.<Context>absent())).build())
                                .read(read().build())
                                .update(update().build())
                                .delete(delete().build())
                                .create(create().mode(ID_FROM_CLIENT).singleton(true).build())
                                .build()).build()
                ).build()).build();
    }

    /**
     * Reads config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Read(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleRead(Context serverContext) {
        String resourceId = resourceId();
        try {

            ServiceConfig config = getServiceConfigNode(serverContext, resourceId);
            if (serviceNotAssignedToRealm(config, serverContext)) {
                throw new NotFoundException();
            }
            String realm = realmFor(serverContext);
            JsonValue result = getJsonValue(realm, config, serverContext);
            return newResultPromise(newResourceResponse(resourceId, String.valueOf(result.hashCode()), result));
        } catch (SMSException | SSOException | IdRepoException | InternalServerErrorException e) {
            debug.warning("::SmsSingletonProvider:: {} on Read", e.getClass().getSimpleName(), e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        } catch (NotFoundException  e) {
            return e.asPromise();
        }
    }

    private boolean serviceNotAssignedToRealm(ServiceConfig config, Context serviceContext)
            throws IdRepoException, SSOException {
        return (type != SchemaType.GLOBAL)
                && (config == null || !config.exists())
                && dynamicSchema != null
                && !isAssignedIdentityService(serviceName,
                        getRealmIdentity(realmFor(serviceContext), getSsoToken(serviceContext)));
    }

    protected Map<String, Set<String>> getDynamicAttributes(String realm) {
        Map<String, Set<String>> result = new HashMap<>();
        result.putAll(dynamicSchema.getAttributeDefaults());
        result.putAll(AuthD.getAuth().getOrgServiceAttributes(realm, serviceName));
        return result;
    }

    @Override
    protected void addDynamicAttributes(String realm, JsonValue result) {
        if (dynamicConverter != null) {
            result.add("dynamic", dynamicConverter.toJson(realm, getDynamicAttributes(realm), true).getObject());
        }
    }

    private void updateDynamicAttributes(Context context, JsonValue value) throws SMSException, SSOException,
            IdRepoException, ResourceException {
        String realm = realmFor(context);
        Map<String, Set<String>> dynamic = dynamicConverter.fromJson(realm, value.get("dynamic"));
        if (SchemaType.GLOBAL.equals(type)) {
            dynamicSchema.setAttributeDefaults(dynamic);
        } else {
            AuthD.getAuth().setOrgServiceAttributes(realm, serviceName, dynamic);
        }
    }

    /**
     * Updates config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Update(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context serverContext,
            UpdateRequest updateRequest) {
        String resourceId = resourceId();
        try {
            if (dynamicSchema != null) {
                updateDynamicAttributes(serverContext, updateRequest.getContent());
            }

            ServiceConfig config = getServiceConfigNode(serverContext, resourceId);
            String realm = realmFor(serverContext);
            saveConfigAttributes(config, convertFromJson(updateRequest.getContent(), realm));

            JsonValue result = getJsonValue(realm, config, serverContext);
            return newResultPromise(newResourceResponse(resourceId, String.valueOf(result.hashCode()), result));
        } catch (SMSException | SSOException | IdRepoException e) {
            debug.warning("::SmsSingletonProvider:: {} on Update", e.getClass().getSimpleName(), e);
            return new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            debug.warning("::SmsSingletonProvider:: {} on Update", e.getClass().getSimpleName(), e);
            return e.asPromise();
        }
    }

    /**
     * Deletes config for the singleton instance referenced.
     * {@inheritDoc}
     */
    @Delete(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleDelete(Context serverContext) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    scm.removeGlobalConfiguration(null);
                } else {
                    if (dynamicSchema != null) {
                        SSOToken ssoToken = getSsoToken(serverContext);

                        AMIdentity realmIdentity = getRealmIdentity(realmFor(serverContext), ssoToken);
                        if (isAssignedIdentityService(serviceName, realmIdentity)) {
                            realmIdentity.unassignService(serviceName);
                        }
                    }
                    scm.deleteOrganizationConfig(realmFor(serverContext));
                }
            } else {
                ServiceConfig parent = parentSubConfigFor(serverContext, scm);
                parent.removeSubConfig(resourceId());
            }
            return newResultPromise(newResourceResponse(resourceId(), "0", json(object(field("success", true)))));
        } catch (SMSException | SSOException | IdRepoException e) {
            debug.warning("::SmsSingletonProvider:: {} on Delete", e.getClass().getSimpleName(), e);
            return new InternalServerErrorException("Unable to delete SMS config: " + e.getMessage()).asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
    }

    private boolean isAssignedIdentityService(String serviceName, AMIdentity realmIdentity) throws IdRepoException, SSOException {
        Set servicesFromIdRepo = realmIdentity.getAssignedServices();
        return servicesFromIdRepo.contains(serviceName);
    }

    private AMIdentity getRealmIdentity(String realm, SSOToken ssoToken) throws IdRepoException, SSOException {
        AMIdentityRepository repo = idRepoFactory.create(realm, ssoToken);
        return repo.getRealmIdentity();
    }

    /**
     * Creates config for the singleton instance referenced, and returns the JsonValue representation.
     * {@inheritDoc}
     */
    @Create(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> handleCreate(Context serverContext,
            CreateRequest createRequest) {
        final String realm = realmFor(serverContext);
        SSOToken ssoToken = getSsoToken(serverContext);
        try {
            JsonValue content = createRequest.getContent();

            Map<String, Set<String>> attrsDynamic = convertFromJsonDynamic(content, realm);
            Map<String, Set<String>> attrsDefaultAndGlobal = convertFromJson(content, realm);
            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            ServiceConfig config = null;
            if (subSchemaPath.isEmpty()) {
                if (type == SchemaType.GLOBAL) {
                    config = scm.createGlobalConfig(attrsDefaultAndGlobal);
                } else {
                    if (serviceHasDefaultOrGlobalSchema()) {
                        if (scm.getOrganizationConfig(realm, null).exists()) {
                            if (isContractConformantUserProvidedIdCreate(serverContext, createRequest)) {
                                return new PreconditionFailedException("Service Already Exists").asPromise();
                            } else {
                                return new ConflictException("Service already exists").asPromise();
                            }
                        } else {
                            config = scm.createOrganizationConfig(realm, attrsDefaultAndGlobal);
                            if (dynamicSchema != null) {
                                AMIdentity realmIdentity = getRealmIdentity(realm, ssoToken);
                                if (isAssignableIdentityService(serviceName, realmIdentity)) {
                                    AuthD.getAuth().setOrgServiceAttributes(realm, serviceName, attrsDynamic);
                                }
                            }
                        }
                    } else {
                        if (serviceNotAssignedToRealm(null, serverContext)) {
                            AuthD.getAuth().setOrgServiceAttributes(realm, serviceName, attrsDynamic);
                        } else {
                            if (isContractConformantUserProvidedIdCreate(serverContext, createRequest)) {
                                return new PreconditionFailedException("Service Already Exists").asPromise();
                            } else {
                                return new ConflictException("Service already exists").asPromise();
                            }
                        }
                        config = null;
                    }
                }
            } else {
                ServiceConfig parent = parentSubConfigFor(serverContext, scm);
                parent.addSubConfig(resourceId(), lastSchemaNodeName(), -1, attrsDefaultAndGlobal);
                config = parent.getSubConfig(lastSchemaNodeName());
            }
            JsonValue result = getJsonValue(realm, config, serverContext);
            return newResultPromise(newResourceResponse(resourceId(), String.valueOf(result.hashCode()), result));
        } catch (SMSException | SSOException | IdRepoException e) {
            debug.warning("::SmsSingletonProvider:: {} on Create", e.getClass().getSimpleName(), e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            debug.warning("::SmsSingletonProvider:: {} on Create", e.getClass().getSimpleName(), e);
            return e.asPromise();
        }
    }

    private boolean isAssignableIdentityService(String serviceName, AMIdentity realmIdentity)
            throws IdRepoException, SSOException {
        Set servicesFromIdRepo = realmIdentity.getAssignableServices();
        return servicesFromIdRepo.contains(serviceName);
    }

    private SSOToken getSsoToken(Context serverContext) {
        return serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();
    }

    @Override
    protected void addDynamicSchema(Optional<Context> context, JsonValue result) {
        if (dynamicSchema != null) {
            addAttributeSchema(result, "/properties/dynamic/properties/", dynamicSchema, context);
            if (result.isDefined("properties") && result.get("properties").isDefined("dynamic")) {
                result.put(new JsonPointer("/properties/dynamic/type"), "object");
                result.put(new JsonPointer("/properties/dynamic/title"),
                        new LocalizableString("i18n:amConsole#section.label.common.dynamicAttributes",
                                this.getClass().getClassLoader()));
            }
        }
    }

    /**
     * Enriches the json response received from the super class with dynamic attribute defaults.
     *
     * @return json response data
     */
    @Override
    protected final JsonValue createTemplate() {
        JsonValue result = super.createTemplate();
        if (dynamicSchema != null) {
            //when retrieving the template we don't want to validate the attributes
            result.add("dynamic",  dynamicConverter.toJson(dynamicSchema.getAttributeDefaults(), false).getObject());
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
    protected ServiceConfig getServiceConfigNode(Context serverContext, String resourceId) throws SSOException,
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
            result = checkedInstanceSubConfig(serverContext, resourceId, config);
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

    protected JsonValue preprocessJsonValue(JsonValue value) {
        value.remove("defaults");
        value.remove("dynamic");
        return value;
    }

    private Map<String, Set<String>> convertFromJson(JsonValue value, String realm) throws ResourceException {
        preprocessJsonValue(value);
        return converter.fromJson(realm, value);
    }

    private Map<String, Set<String>> convertFromJsonDynamic(JsonValue value, String realm) throws ResourceException {
        if (dynamicSchema == null) {
            return Collections.emptyMap();
        }
        return dynamicConverter.fromJson(realm, value.get("dynamic"));
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
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return description;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return description;
    }

    @Override
    public void addDescriptorListener(Listener listener) {

    }

    @Override
    public void removeDescriptorListener(Listener listener) {

    }
}
