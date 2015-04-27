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
package org.forgerock.openam.rest.scripting;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.scripting.ScriptConstants.*;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.entitlements.RealmAwareResource;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.scripting.ScriptError;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.ScriptValidator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A REST endpoint for managing scripts in OpenAM.
 *
 * @since 13.0.0
 */
public class ScriptResource extends RealmAwareResource {

    private final Logger logger;
    private final ScriptingServiceFactory<ScriptConfiguration> serviceFactory;
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
                          ScriptingServiceFactory<ScriptConfiguration> scriptConfigService,
                          ExceptionMappingHandler<ScriptException, ResourceException> exceptionMappingHandler,
                          ScriptValidator scriptValidator) {
        this.logger = logger;
        this.serviceFactory = scriptConfigService;
        this.exceptionMappingHandler = exceptionMappingHandler;
        this.scriptValidator = scriptValidator;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> resultHandler) {
        if ("validate".equals(request.getAction())) {
            try {
                JsonValue json = request.getContent();
                SupportedScriptingLanguage language = getLanguageFromString(json.get(SCRIPT_LANGUAGE).asString());
                String script = json.get(SCRIPT_TEXT).asString();
                if (script == null) {
                    throw new ScriptException(ScriptErrorCode.MISSING_SCRIPT);
                }

                List<ScriptError> scriptErrorList = scriptValidator.validateScript(new ScriptObject(EMPTY,
                        Base64.decodeAsUTF8String(script), language, null));
                if (scriptErrorList.isEmpty()) {
                    resultHandler.handleResult(json(object(field("success", true))));
                    return;
                }

                Set<Object> errors = new HashSet<>();
                for (ScriptError error : scriptErrorList) {
                    errors.add(object(
                            field("line", error.getLineNumber()),
                            field("column", error.getColumnNumber()),
                            field("message", error.getMessage())));
                }
                resultHandler.handleResult(json(object(field("success", false), field("errors", errors))));
            } catch (ScriptException se) {
                resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
            }
        } else {
            resultHandler.handleError(new NotSupportedException());
        }
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> resultHandler) {
        try {
             final ScriptConfiguration sc = serviceFactory
                    .create(getContextSubject(context), getRealm(context))
                    .create(fromJson(request.getContent()));
            resultHandler.handleResult(new Resource(sc.getId(), String.valueOf(sc.hashCode()), asJson(sc)));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
        }
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> resultHandler) {

        try {
            serviceFactory.create(getContextSubject(context), getRealm(context)).delete(resourceId);
            resultHandler.handleResult(new Resource(resourceId, null, json(object())));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
        }

    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler resultHandler) {
        resultHandler = QueryResultHandlerBuilder.withPagingAndSorting(resultHandler, request);
        final QueryFilter filter = request.getQueryFilter();
        try {
            final Set<ScriptConfiguration> configs;
            if (filter == null) {
                configs = serviceFactory.create(getContextSubject(context), getRealm(context)).getAll();
            } else {
                configs = serviceFactory.create(getContextSubject(context), getRealm(context)).get(filter);
            }
            for (ScriptConfiguration sc : configs) {
                resultHandler.handleResource(new Resource(sc.getId(), String.valueOf(sc.hashCode()), asJson(sc)));
            }
            resultHandler.handleResult(new QueryResult());
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> resultHandler) {
        try {
            resultHandler.handleResult(new Resource(resourceId, null, asJson(
                    serviceFactory.create(getContextSubject(context), getRealm(context)).get(resourceId))));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> resultHandler) {
        try {
            resultHandler.handleResult(new Resource(resourceId, null, asJson(serviceFactory
                    .create(getContextSubject(context), getRealm(context))
                    .update(fromJson(request.getContent(), resourceId)))));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(context, request, se));
        }
    }

    /**
     * Convert the {@code ScriptConfiguration} into a {@code JsonValue} instance.
     * @param scriptConfig The {@code ScriptConfiguration}.
     * @return The {@code JsonValue}.
     */
    private JsonValue asJson(ScriptConfiguration scriptConfig) throws ScriptException {
        try {
            return json(object(field(JSON_UUID, scriptConfig.getId()),
                    field(SCRIPT_NAME, scriptConfig.getName()),
                    field(SCRIPT_DESCRIPTION, scriptConfig.getDescription()),
                    field(SCRIPT_TEXT, Base64.encode(scriptConfig.getScript().getBytes("UTF-8"))),
                    field(SCRIPT_LANGUAGE, scriptConfig.getLanguage().name()),
                    field(SCRIPT_CONTEXT, scriptConfig.getContext().name()),
                    field(SCRIPT_CREATED_BY, scriptConfig.getCreatedBy()),
                    field(SCRIPT_CREATION_DATE, scriptConfig.getCreationDate()),
                    field(SCRIPT_LAST_MODIFIED_BY, scriptConfig.getLastModifiedBy()),
                    field(SCRIPT_LAST_MODIFIED_DATE, scriptConfig.getLastModifiedDate())));
        } catch (UnsupportedEncodingException e) {
            throw ScriptException.createAndLogError(logger, ScriptErrorCode.SCRIPT_ENCODING_FAILED, e, "UTF-8");
        }
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
        final String script = jsonValue.get(SCRIPT_TEXT).asString();
        final ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .setId(uuid)
                .setName(jsonValue.get(SCRIPT_NAME).asString())
                .setDescription(jsonValue.get(SCRIPT_DESCRIPTION).asString())
                .setScript(script == null ? null : Base64.decodeAsUTF8String(script))
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
}
