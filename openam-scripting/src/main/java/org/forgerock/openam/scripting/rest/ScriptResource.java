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
package org.forgerock.openam.scripting.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SCRIPT_RESOURCE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.scripting.ScriptConstants.*;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.MISSING_SCRIPT;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.SCRIPT_DECODING_FAILED;
import static org.forgerock.openam.utils.CollectionUtils.newList;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.RealmAwareResource;
import org.forgerock.openam.rest.query.QueryByStringFilterConverter;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.scripting.ScriptError;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.ScriptValidator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.encode.Base64;

/**
 * A REST endpoint for managing scripts in OpenAM.
 *
 * @since 13.0.0
 */
@CollectionProvider(
        details = @Handler(
                title = SCRIPT_RESOURCE + TITLE,
                description = SCRIPT_RESOURCE + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(schemaResource = "ScriptResource.schema.json")),
        pathParam = @Parameter(
                name = "scriptId",
                type = "string",
                description = SCRIPT_RESOURCE + PATH_PARAM + DESCRIPTION))
public class ScriptResource extends RealmAwareResource {

    private final Logger logger;
    private final ScriptingServiceFactory serviceFactory;
    private final ExceptionMappingHandler<ScriptException, ResourceException> exceptionMappingHandler;
    private final ScriptValidator scriptValidator;

    /**
     * Creates an instance of the {@code ScriptResource}.
     *
     * @param scriptConfigService An instance of the {@code ScriptConfigurationService}.
     * @param exceptionMappingHandler An instance of the {@code ExceptionMappingHandler}.
     */
    @Inject
    public ScriptResource(@Named("ScriptLogger") Logger logger,
                          ScriptingServiceFactory scriptConfigService,
                          ExceptionMappingHandler<ScriptException, ResourceException> exceptionMappingHandler,
                          ScriptValidator scriptValidator) {
        this.logger = logger;
        this.serviceFactory = scriptConfigService;
        this.exceptionMappingHandler = exceptionMappingHandler;
        this.scriptValidator = scriptValidator;
    }

    @Override
    @Action(name = "validate",
            operationDescription = @Operation(
                    errors = {
                            @ApiError(code = 400,
                                    description = SCRIPT_RESOURCE + "error.missing.script"),
                            @ApiError(code = 400,
                                    description = SCRIPT_RESOURCE + "error.script.decode"),
                            @ApiError(code = 400,
                                    description = SCRIPT_RESOURCE + "error.script.language.not.supported")},
                    description = SCRIPT_RESOURCE + "validate.action." + DESCRIPTION),
            request = @Schema(schemaResource = "ScriptResource.action.validate.request.schema.json"),
            response = @Schema(schemaResource = "ScriptResource.action.validate.response.schema.json"))
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        if ("validate".equals(request.getAction())) {
            try {
                JsonValue json = request.getContent();
                SupportedScriptingLanguage language = getLanguageFromString(json.get(SCRIPT_LANGUAGE).asString());
                String script = json.get(SCRIPT_TEXT).asString();
                if (script == null) {
                    throw new ScriptException(MISSING_SCRIPT);
                }

                List<ScriptError> scriptErrorList = scriptValidator.validateScript(new ScriptObject(EMPTY,
                        decodeScript(script), language, null));
                if (scriptErrorList.isEmpty()) {
                    return newResultPromise(newActionResponse(json(object(field("success", true)))));
                }

                Set<Object> errors = new HashSet<>();
                for (ScriptError error : scriptErrorList) {
                    errors.add(object(
                            field("line", error.getLineNumber()),
                            field("column", error.getColumnNumber()),
                            field("message", error.getMessage())));
                }
                return newResultPromise(newActionResponse(json(object(
                        field("success", false),
                        field("errors", newList(errors))))));
            } catch (ScriptException se) {
                return exceptionMappingHandler.handleError(context, request, se).asPromise();
            }
        } else {
            return new NotSupportedException().asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    @Create(operationDescription = @Operation(
            errors = {
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.decode")},
            description = SCRIPT_RESOURCE + CREATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        try {
            if (request.getNewResourceId() != null) {
                return new NotSupportedException("IDs for scripts are generated and cannot be provided").asPromise();
            }
            final ScriptConfiguration sc = serviceFactory
                    .create(getRealm(context))
                    .create(fromJson(request.getContent()), getContextSubject(context));
            return newResultPromise(newResourceResponse(sc.getId(), String.valueOf(sc.hashCode()), asJson(sc)));
        } catch (ScriptException se) {
            return exceptionMappingHandler.handleError(context, request, se).asPromise();
        }
    }

    @Override
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.cannot.find.realm"),
                    @ApiError(code = 403,
                            description = SCRIPT_RESOURCE + "error.cannot.delete.default.script"),
                    @ApiError(code = 404,
                            description = SCRIPT_RESOURCE + "error.script.not.found"),
                    @ApiError(code = 500,
                            description = SCRIPT_RESOURCE + "error.delete.script.used.once"),
                    @ApiError(code = 500,
                            description = SCRIPT_RESOURCE + "error.delete.script.used.multiple"),
                    @ApiError(code = 500,
                            description = SCRIPT_RESOURCE + "error.delete.failed")},
            description = SCRIPT_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {

        try {
            serviceFactory.create(getRealm(context)).delete(resourceId);
            return newResultPromise(newResourceResponse(resourceId, null, json(object())));
        } catch (ScriptException se) {
            return exceptionMappingHandler.handleError(context, request, se).asPromise();
        }
    }

    @Override
    @Query(operationDescription = @Operation(
            description = SCRIPT_RESOURCE + QUERY_DESCRIPTION),
        type = QueryType.FILTER,
        queryableFields = "*")
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler resultHandler) {
        final QueryFilter<JsonPointer> filter = request.getQueryFilter();
        try {
            final Set<ScriptConfiguration> configs;
            if (filter == null) {
                configs = serviceFactory.create(getRealm(context)).getAll();
            } else {
                QueryFilter<String> stringQueryFilter = filter.accept(
                        new QueryByStringFilterConverter(), null);
                configs = serviceFactory.create(getRealm(context)).get(stringQueryFilter);
            }

            List<ResourceResponse> results = new ArrayList<>();
            for (ScriptConfiguration configuration : configs) {
                String id = configuration.getId();
                results.add(newResourceResponse(id, null, asJson(configuration)));
            }

            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
            return QueryResponsePresentation.perform(resultHandler, request, results);
        } catch (ScriptException se) {
            return exceptionMappingHandler.handleError(context, request, se).asPromise();
        }
    }

    @Override
    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(code = 400,
                            description = 
                                    SCRIPT_RESOURCE + "error.script.not.found")},
            description = SCRIPT_RESOURCE + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        try {
            return newResultPromise(newResourceResponse(resourceId, null, asJson(
                    serviceFactory.create(getRealm(context)).get(resourceId))));
        } catch (ScriptException se) {
            return exceptionMappingHandler.handleError(context, request, se).asPromise();
        }
    }

    @Override
    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.decode"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.language.not.supported"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.type.not.found"),
                    @ApiError(code = 404,
                            description = SCRIPT_RESOURCE + "error.script.not.found"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.name.empty"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.not.specified"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.language.not.spec"),
                    @ApiError(code = 400,
                            description = SCRIPT_RESOURCE + "error.script.type.not.spec")},
            description = SCRIPT_RESOURCE + UPDATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        try {
            return newResultPromise(newResourceResponse(resourceId, null, asJson(serviceFactory
                    .create(getRealm(context))
                    .update(fromJson(request.getContent(), resourceId), getContextSubject(context)))));
        } catch (ScriptException se) {
            return exceptionMappingHandler.handleError(context, request, se).asPromise();
        }
    }

    /**
     * Convert the {@code ScriptConfiguration} into a {@code JsonValue} instance.
     * @param scriptConfig The {@code ScriptConfiguration}.
     * @return The {@code JsonValue}.
     */
    private JsonValue asJson(ScriptConfiguration scriptConfig) {
        return json(object(field(JSON_UUID, scriptConfig.getId()),
                field(SCRIPT_NAME, scriptConfig.getName()),
                field(SCRIPT_DESCRIPTION, scriptConfig.getDescription()),
                field(SCRIPT_TEXT, Base64.encode(scriptConfig.getScript().getBytes(UTF_8))),
                field(SCRIPT_IS_DEFAULT, scriptConfig.isDefault()),
                field(SCRIPT_LANGUAGE, scriptConfig.getLanguage().name()),
                field(SCRIPT_CONTEXT, scriptConfig.getContext().name()),
                field(SCRIPT_CREATED_BY, scriptConfig.getCreatedBy()),
                field(SCRIPT_CREATION_DATE, scriptConfig.getCreationDate()),
                field(SCRIPT_LAST_MODIFIED_BY, scriptConfig.getLastModifiedBy()),
                field(SCRIPT_LAST_MODIFIED_DATE, scriptConfig.getLastModifiedDate())));
    }

    /**
     * Convert the {@code JsonValue} into a {@code ScriptConfiguration} instance.
     * If the given UUID is {@code null} a new one will be generated.
     * @param jsonValue The {@code JsonValue}.
     * @param uuid The UUID for an existing script configuration.
     * @return The {@code ScriptConfiguration}.
     * @throws ScriptException When the given JSON value does not represent a script configuration.
     */
    private ScriptConfiguration fromJson(JsonValue jsonValue, String uuid) throws ScriptException {
        final String language = jsonValue.get(SCRIPT_LANGUAGE).asString();
        final String context = jsonValue.get(SCRIPT_CONTEXT).asString();
        final ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .setId(uuid)
                .setName(jsonValue.get(SCRIPT_NAME).asString())
                .setDescription(jsonValue.get(SCRIPT_DESCRIPTION).asString())
                .setScript(decodeScript(jsonValue.get(SCRIPT_TEXT).asString()))
                .setLanguage(language == null ? null : getLanguageFromString(language))
                .setContext(context == null ? null : getContextFromString(context));
        if (uuid == null) {
            builder.generateId();
        }
        return builder.build();
    }

    /**
     * Convert the {@code JsonValue} into a {@code ScriptConfiguration} instance. The UUID for the script
     * configuration will be generated.
     * @param jsonValue The {@code JsonValue}.
     * @return The {@code ScriptConfiguration}.
     * @throws ScriptException When the given JSON does not represent a script configuration.
     */
    private ScriptConfiguration fromJson(JsonValue jsonValue) throws ScriptException {
        return fromJson(jsonValue, null);
    }

    private String decodeScript(String encodedScript) throws ScriptException {
        if (encodedScript == null) {
            return null;
        }

        byte[] decodedScript = Base64.decode(encodedScript);

        if (decodedScript == null) {
            throw ScriptException.createAndLogError(logger, SCRIPT_DECODING_FAILED);
        }

        return new String(decodedScript, UTF_8);
    }
}
