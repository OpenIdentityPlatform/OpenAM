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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.authentication.config.AMAuthenticationManager.getAuthenticationServiceNames;
import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.enums.ParameterSource.PATH;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.Create.create;
import static org.forgerock.api.models.Delete.delete;
import static org.forgerock.api.models.Items.items;
import static org.forgerock.api.models.Parameter.parameter;
import static org.forgerock.api.models.Query.query;
import static org.forgerock.api.models.Read.read;
import static org.forgerock.api.models.Update.update;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SMS_RESOURCE_PROVIDER;
import static org.forgerock.openam.rest.RestUtils.crestProtocolVersion;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import com.google.common.base.Optional;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.ResultHandler;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;

/**
 * A CREST collection provider for SMS schema config.
 * @since 13.0.0
 */
@CollectionProvider(details = @Handler(mvccSupported = false))
public class SmsCollectionProvider extends SmsResourceProvider {

    private static final Collection<Pattern> UNCREATABLE_TYPES =
            Collections.singleton(Pattern.compile("services\\/scripting\\/contexts$"));
    private static final Collection<Pattern> UNDELETABLE_TYPES =
            Collections.singleton(Pattern.compile("services\\/scripting\\/contexts\\/[\\w]+$"));
    private static final LocalizableString QUERY_DESCRIPTION =
            new LocalizableString(SMS_RESOURCE_PROVIDER + "collection.query.description");

    private final boolean autoCreatedAuthModule;
    private final String authModuleResourceName;
    private final ApiDescription description;

    @Inject
    SmsCollectionProvider(@Assisted SmsJsonConverter converter, @Assisted ServiceSchema schema,
            @Assisted SchemaType type, @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName, @Named("frRest") Debug debug,
            @Named("AMResourceBundleCache") AMResourceBundleCache resourceBundleCache,
            @Named("DefaultLocale") Locale defaultLocale) {
        super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug, resourceBundleCache,
                defaultLocale);
        Reject.ifTrue(type != SchemaType.GLOBAL && type != SchemaType.ORGANIZATION, "Unsupported type: " + type);
        Reject.ifTrue(subSchemaPath.isEmpty(), "Root schemas do not support multiple instances");
        autoCreatedAuthModule = subSchemaPath.size() == 1 && getAuthenticationServiceNames().contains(serviceName) &&
                super.uriPath.size() == 1 && AUTO_CREATED_AUTHENTICATION_MODULES.containsValue(super.uriPath.get(0));
        authModuleResourceName = autoCreatedAuthModule ? super.uriPath.get(0) : null;
        description = ApiDescription.apiDescription().id("fake").version("v")
                .paths(Paths.paths().put("", VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, Resource.resource()
                                .title(getI18NName())
                                .description(getSchemaDescription(schema.getI18NKey()))
                                .mvccSupported(false)
                                .items(items().pathParameter(parameter().name("id").type("string").source(PATH).build())
                                        .read(read().build())
                                        .update(update().build())
                                        .delete(delete().build())
                                        .create(create().mode(ID_FROM_CLIENT).build())
                                        .build())
                                .resourceSchema(Schema.schema().schema(
                                        createSchema(Optional.<Context>absent())).build())
                                .resourceSchema(Schema.schema().schema(createSchema(Optional.<Context>absent())).build())
                                .query(query().type(QueryType.FILTER)
                                        .description(QUERY_DESCRIPTION).queryableFields().build())
                                .create(create().mode(ID_FROM_CLIENT).build())
                                .action(action().name("schema").description(SCHEMA_DESCRIPTION).build())
                                .action(action().name("template").description(TEMPLATE_DESCRIPTION).build())
                                .build()).build()
                ).build()).build();
    }

    @Action(operationDescription = @Operation, name = "nextdescendents")
    public Promise<ActionResponse, ResourceException> nextDescendentsAction(Context context, String resourceId,
            ActionRequest request) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            ServiceConfig contextConfig = config.getSubConfig(getUriTemplateVariables(context).get("id"));

            JsonValue result = json(object(field("result", array())));
            for (String subConfigName : contextConfig.getSubConfigNames("*")) {
                ServiceConfig subConfig = contextConfig.getSubConfig(subConfigName);
                ServiceSchema subSchema = schema.getSubSchema(subConfigName);
                JsonValue value = new SmsCollectionProvider(new SmsJsonConverter(subSchema), subSchema,
                        subSchema.getServiceType(), subSchemaPath, null, false, debug, resourceBundleCache,
                        defaultLocale).getJsonValue(null, subConfig, context);
                result.get("result").add(value.getObject());
            }

            return newActionResponse(result).asPromise();
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on nextdescendents action", e);
            return new InternalServerErrorException("Unable to perform nextdescendents action SMS config: "
                    + e.getMessage()).asPromise();
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on nextdescendents action", e);
            return new InternalServerErrorException("Unable to perform nextdescendents action SMS config: "
                    + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Action(operationDescription = @Operation, name = "getCreatableTypes")
    public Promise<ActionResponse, ResourceException> getCreatableTypes(Context context, String actionId, ActionRequest request) {
        return newActionResponse(json(array())).asPromise();
    }

    /**
     * Creates a new child instance of config. The parent config referenced by the request path is found, and
     * new config is created using the provided name property.
     * {@inheritDoc}
     */
    @Create(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, CreateRequest request) {
        if (matchesPattern(UNCREATABLE_TYPES, context.asContext(UriRouterContext.class).getBaseUri())) {
            return new NotSupportedException().asPromise();
        }
        JsonValue content = request.getContent();
        final String realm = realmFor(context);
        try {
            Map<String, Set<String>> attrs = converter.fromJson(realm, content);
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            String name = content.get("_id").asString();
            if (name == null) {
                name = request.getNewResourceId();
            } else if (request.getNewResourceId() != null && !name.equals(request.getNewResourceId())) {
                return new BadRequestException("name and URI's resource ID do not match").asPromise();
            }
            if (name == null) {
                return new BadRequestException("Invalid name").asPromise();
            }
            config.addSubConfig(name, lastSchemaNodeName(), 0, attrs);
            final ServiceConfig created = checkedInstanceSubConfig(context, name, config);

            return awaitCreation(context, name)
                    .then(new Function<Void, ResourceResponse, ResourceException>() {
                        @Override
                        public ResourceResponse apply(Void aVoid) {
                            JsonValue result = null;
                            try {
                                result = getJsonValue(realm, created, context, authModuleResourceName,
                                        autoCreatedAuthModule);
                            } catch (InternalServerErrorException e) {
                                debug.warning("Error creating JsonValue", e);
                            }
                            return newResourceResponse(created.getName(), String.valueOf(result.hashCode()), result);
                        }
                    });
        } catch (ServiceAlreadyExistsException e) {
            debug.warning("::SmsCollectionProvider:: ServiceAlreadyExistsException on create", e);
            if (isContractConformantUserProvidedIdCreate(context, request)) {
                return new PreconditionFailedException("Unable to create SMS config: " + e.getMessage()).asPromise();
            } else {
                return new ConflictException("Unable to create SMS config: " + e.getMessage()).asPromise();
            }
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on create", e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on create", e);
            return new InternalServerErrorException("Unable to create SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private boolean matchesPattern(Collection<Pattern> patterns, String baseUri) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(baseUri).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a child instance of config. The parent config referenced by the request path is found, and
     * the config is deleted using the resourceId.
     * {@inheritDoc}
     */
    @Delete(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, final String resourceId) {
        if (matchesPattern(UNDELETABLE_TYPES, context.asContext(UriRouterContext.class).getBaseUri())) {
            return new NotSupportedException().asPromise();
        }
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            checkedInstanceSubConfig(context, resourceId, config);
            if (isDefaultCreatedAuthModule(context, resourceId)) {
                scm.removeOrganizationConfiguration(realmFor(context), null);
            } else {
                config.removeSubConfig(resourceId);
            }

            return awaitDeletion(context, resourceId)
                    .then(new Function<Void, ResourceResponse, ResourceException>() {
                        @Override
                        public ResourceResponse apply(Void aVoid) {
                            return newResourceResponse(resourceId, "0", json(object(field("success", true))));
                        }
                    });
        } catch (ServiceNotFoundException e) {
            debug.warning("::SmsCollectionProvider:: ServiceNotFoundException on delete", e);
            return new NotFoundException("Unable to delete SMS config: " + e.getMessage()).asPromise();
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on delete", e);
            return new InternalServerErrorException("Unable to delete SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on delete", e);
            return new InternalServerErrorException("Unable to delete SMS config: " + e.getMessage()).asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
    }

    /**
     * Reads a child instance of config. The parent config referenced by the request path is found, and
     * the config is read using the resourceId.
     * {@inheritDoc}
     */
    @Read(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId) {
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            ServiceConfig item = checkedInstanceSubConfig(context, resourceId, config);

            JsonValue result = getJsonValue(realmFor(context), item, context, authModuleResourceName,
                    autoCreatedAuthModule);
            return newResultPromise(newResourceResponse(resourceId, String.valueOf(result.hashCode()), result));
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on read", e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException | InternalServerErrorException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on read", e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
    }

    /**
     * Updates a child instance of config. The parent config referenced by the request path is found, and
     * the config is updated using the resourceId.
     * {@inheritDoc}
     */
    @Update(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        JsonValue content = request.getContent();
        String realm = realmFor(context);
        try {
            Map<String, Set<String>> attrs = converter.fromJson(realm, content);
            ServiceConfigManager scm = getServiceConfigManager(context);
            ServiceConfig config = parentSubConfigFor(context, scm);
            ServiceConfig node = checkedInstanceSubConfig(context, resourceId, config);

            node.setAttributes(attrs);
            JsonValue result = getJsonValue(realm, node, context, authModuleResourceName, autoCreatedAuthModule);
            return newResultPromise(newResourceResponse(resourceId, String.valueOf(result.hashCode()), result));
        } catch (ServiceNotFoundException e) {
            debug.warning("::SmsCollectionProvider:: ServiceNotFoundException on update", e);
            return new NotFoundException("Unable to update SMS config: " + e.getMessage()).asPromise();
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on update", e);
            return new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on update", e);
            return new InternalServerErrorException("Unable to update SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
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
    @Query(operationDescription = @Operation, type = QueryType.FILTER, queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new NotSupportedException("Query not supported: " + request.getQueryFilter()).asPromise();
        }
        if (request.getPagedResultsCookie() != null || request.getPagedResultsOffset() > 0 ||
                request.getPageSize() > 0) {
            return new NotSupportedException("Query paging not currently supported").asPromise();
        }
        try {
            ServiceConfigManager scm = getServiceConfigManager(context);
            String realm = realmFor(context);
            if (subSchemaPath.isEmpty()) {
                Set<String> instanceNames = new TreeSet<String>(scm.getInstanceNames());
                for (String instanceName : instanceNames) {
                    ServiceConfig config = type == SchemaType.GLOBAL ? scm.getGlobalConfig(instanceName) :
                            scm.getOrganizationConfig(realm, instanceName);
                    if (config != null) {
                        handleServiceConfig(handler, realm, instanceName, config, context);
                    }
                }
            } else {
                ServiceConfig config = parentSubConfigFor(context, scm);
                Set<String> names = config.getSubConfigNames("*", lastSchemaNodeName());
                for (String configName : names) {
                    ServiceConfig subConfig = config.getSubConfig(configName);
                    handleServiceConfig(handler, realm, configName, subConfig, context);
                }
                if (autoCreatedAuthModule) {
                    String instanceName = AUTO_CREATED_AUTHENTICATION_MODULES.inverse().get(authModuleResourceName);
                    handleServiceConfig(handler, realm, instanceName, config, context);
                }
            }

            return newResultPromise(newQueryResponse());
        } catch (SMSException e) {
            debug.warning("::SmsCollectionProvider:: SMSException on query", e);
            return new InternalServerErrorException("Unable to query SMS config: " + e.getMessage()).asPromise();
        } catch (SSOException | InternalServerErrorException e) {
            debug.warning("::SmsCollectionProvider:: SSOException on query", e);
            return new InternalServerErrorException("Unable to query SMS config: " + e.getMessage()).asPromise();
        } catch (NotFoundException e) {
            return e.asPromise();
        }
    }

    private void handleServiceConfig(QueryResourceHandler handler, String realm, String configName, ServiceConfig
            subConfig, Context context) throws InternalServerErrorException {
        JsonValue value = getJsonValue(realm, subConfig, context, authModuleResourceName, autoCreatedAuthModule);
        handler.handleResource(newResourceResponse(configName, String.valueOf(value.hashCode()), value));
    }

    private static final long MAX_AWAIT_TIMEOUT = 5000L;

    private Promise<Void, ResourceException> awaitCreation(Context context, String resourceId) {
        final PromiseImpl<Void, ResourceException> awaitPromise = PromiseImpl.create();
        await(context, resourceId, awaitPromise, currentTimeMillis(), awaitCreationResultHandler(awaitPromise),
                awaitCreationExceptionHandler(context, resourceId, awaitPromise, currentTimeMillis()));
        return awaitPromise;
    }

    private Promise<Void, ResourceException> awaitDeletion(Context context, String resourceId) {
        final PromiseImpl<Void, ResourceException> awaitPromise = PromiseImpl.create();
        await(context, resourceId, awaitPromise, currentTimeMillis(),
                awaitDeletionResultHandler(context, resourceId, awaitPromise, currentTimeMillis()),
                awaitDeletionExceptionHandler(context, resourceId, awaitPromise, currentTimeMillis()));
        return awaitPromise;
    }

    private void await(Context context, String resourceId, PromiseImpl<Void, ResourceException> awaitPromise,
            long startTime, ResultHandler<ResourceResponse> resultHandler,
            ExceptionHandler<ResourceException> exceptionHandler) {
        if (currentTimeMillis() - startTime > MAX_AWAIT_TIMEOUT) {
            awaitPromise.handleResult(null);
            return;
        }
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            debug.error("Thread interrupted while awaiting SMS resource creation/deletion", e);
            awaitPromise.handleException(new InternalServerErrorException("", e));
        }
        readInstance(context, resourceId)
                .thenOnResult(resultHandler)
                .thenOnException(exceptionHandler);
    }

    private ResultHandler<ResourceResponse> awaitCreationResultHandler(
            final PromiseImpl<Void, ResourceException> awaitPromise) {
        return new ResultHandler<ResourceResponse>() {
            @Override
            public void handleResult(ResourceResponse result) {
                awaitPromise.handleResult(null);
            }
        };
    }

    private ExceptionHandler<ResourceException> awaitCreationExceptionHandler(final Context context,
            final String resourceId, final PromiseImpl<Void, ResourceException> awaitPromise, final long startTime) {
        return new ExceptionHandler<ResourceException>() {
            @Override
            public void handleException(ResourceException exception) {
                if (ResourceException.NOT_FOUND != exception.getCode()) {
                    debug.warning("Unexpected exception returned while awaiting SMS resource creation", exception);
                }
                await(context, resourceId, awaitPromise, startTime,
                        awaitCreationResultHandler(awaitPromise),
                        awaitCreationExceptionHandler(context, resourceId, awaitPromise, startTime));
            }
        };
    }

    private ResultHandler<ResourceResponse> awaitDeletionResultHandler(final Context context, final String resourceId,
            final PromiseImpl<Void, ResourceException> awaitPromise, final long startTime) {
        return new ResultHandler<ResourceResponse>() {
            @Override
            public void handleResult(ResourceResponse result) {
                await(context, resourceId, awaitPromise, startTime,
                        awaitDeletionResultHandler(context, resourceId, awaitPromise, startTime),
                        awaitDeletionExceptionHandler(context, resourceId, awaitPromise, startTime));
            }
        };
    }

    private ExceptionHandler<ResourceException> awaitDeletionExceptionHandler(final Context context,
            final String resourceId, final PromiseImpl<Void, ResourceException> awaitPromise, final long startTime) {
        return new ExceptionHandler<ResourceException>() {
            @Override
            public void handleException(ResourceException exception) {
                if (ResourceException.NOT_FOUND != exception.getCode()) {
                    debug.warning("Unexpected exception returned while awaiting SMS resource deletion", exception);
                    await(context, resourceId, awaitPromise, startTime,
                            awaitDeletionResultHandler(context, resourceId, awaitPromise, startTime),
                            awaitDeletionExceptionHandler(context, resourceId, awaitPromise, startTime));
                } else {
                    awaitPromise.handleResult(null);
                }
            }
        };
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
