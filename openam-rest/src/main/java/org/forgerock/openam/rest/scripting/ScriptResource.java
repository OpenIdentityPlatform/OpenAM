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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
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
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.entitlements.RealmAwareResource;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptConfigurationService;

import java.util.Set;

/**
 * A REST endpoint for managing scripts in OpenAM.
 *
 * @since 13.0.0
 */
public class ScriptResource extends RealmAwareResource {

    private final ScriptConfigurationService scriptConfigService;
    private final ExceptionMappingHandler<ScriptException, ResourceException> exceptionMappingHandler;

    /**
     * Creates an instance of the {@code ScriptResource}.
     *
     * @param scriptConfigService An instance of the {@code ScriptConfigurationService}.
     * @param exceptionMappingHandler An instance of the {@code ExceptionMappingHandler}.
     */
    @Inject
    public ScriptResource(ScriptConfigurationService scriptConfigService,
                          ExceptionMappingHandler<ScriptException, ResourceException> exceptionMappingHandler) {
        this.scriptConfigService = scriptConfigService;
        this.exceptionMappingHandler = exceptionMappingHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> resultHandler) {
        try {
            final ScriptConfiguration sc = scriptConfigService.saveScriptConfiguration(getContextSubject(context),
                    getRealm(context), fromJson(request.getContent()));
            resultHandler.handleResult(new Resource(sc.getUuid(), null, asJson(sc)));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(se));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> resultHandler) {

        try {
            scriptConfigService.deleteScriptConfiguration(getContextSubject(context), getRealm(context), resourceId);
            resultHandler.handleResult(new Resource(resourceId, null, json(object())));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(se));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler resultHandler) {
        resultHandler = QueryResultHandlerBuilder.withPagingAndSorting(resultHandler, request);
        try {
            final Set<ScriptConfiguration> configs = scriptConfigService.getScriptConfigurations(
                    getContextSubject(context), getRealm(context));
            int remaining = 0;
            if (configs.size() > 0) {
                remaining = configs.size();
                for (ScriptConfiguration sc : configs) {
                    boolean keepGoing = resultHandler.handleResource(new Resource(sc.getUuid(),
                            String.valueOf(System.currentTimeMillis()), asJson(sc)));
                    remaining--;
                    if (!keepGoing) {
                        break;
                    }
                }
            }
            resultHandler.handleResult(new QueryResult(null, remaining));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(se));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                             ResultHandler<Resource> resultHandler) {
        try {
            resultHandler.handleResult(new Resource(resourceId, null, asJson(scriptConfigService.getScriptConfiguration(
                    getContextSubject(context), getRealm(context), resourceId))));
        } catch (ScriptException se) {
            resultHandler.handleError(exceptionMappingHandler.handleError(se));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> resultHandler) {
        try {
            resultHandler.handleResult(new Resource(resourceId, null, asJson(
                    scriptConfigService.updateScriptConfiguration(getContextSubject(context), getRealm(context),
                    fromJson(request.getContent(), resourceId)))));
        } catch (ScriptException se) {
            resultHandler.handleError(new BadRequestException(se.getLocalizedMessage()));
        }
    }

    /**
     * Convert the {@code ScriptConfiguration} into a {@code JsonValue} instance.
     * @param scriptConfig The {@code ScriptConfiguration}.
     * @return The {@code JsonValue}.
     */
    private JsonValue asJson(ScriptConfiguration scriptConfig) {
        return json(object(field(SCRIPT_UUID, scriptConfig.getUuid()),
                field(SCRIPT_NAME, scriptConfig.getName()),
                field(SCRIPT_TEXT, scriptConfig.getScript()),
                field(SCRIPT_LANGUAGE, scriptConfig.getLanguage().name()),
                field(SCRIPT_CONTEXT, scriptConfig.getContext().name())));
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
        final ScriptConfiguration.Builder builder = ScriptConfiguration.builder()
                .setUuid(uuid)
                .setName(jsonValue.get(SCRIPT_NAME).asString())
                .setScript(jsonValue.get(SCRIPT_TEXT).asString())
                .setLanguage(getLanguageFromString(jsonValue.get(SCRIPT_LANGUAGE).asString()))
                .setContext(getContextFromString(jsonValue.get(SCRIPT_CONTEXT).asString()));
        if (uuid == null) {
            builder.generateUuid();
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
