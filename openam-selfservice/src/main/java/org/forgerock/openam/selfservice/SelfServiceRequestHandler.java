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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.selfservice;

import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Read.read;
import static org.forgerock.api.models.Resource.resource;
import static org.forgerock.api.models.Schema.schema;
import static org.forgerock.api.models.VersionedPath.versionedPath;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SELF_SERVICE_REQUEST_HANDLER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.utils.JsonValueBuilder.fromResource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.TranslateJsonSchema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.selfservice.config.SelfServiceConsoleConfig;
import org.forgerock.openam.selfservice.config.ServiceConfigProvider;
import org.forgerock.openam.selfservice.config.ServiceConfigProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.sm.config.ConsoleConfigListener;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Function;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract request handler used to setup the self services.
 *
 * @since 13.0.0
 */
final class SelfServiceRequestHandler<C extends SelfServiceConsoleConfig>
        extends AbstractRequestHandler implements ConsoleConfigListener, Describable<ApiDescription, Request> {

    private static final Logger logger = LoggerFactory.getLogger(SelfServiceRequestHandler.class);
    private static final String GENERIC_I18N_KEY = "per-builder-i18n;";

    private final Class<? extends ConsoleConfigBuilder<C>> consoleConfigBuilderType;
    private final ConsoleConfigHandler consoleConfigHandler;
    private final ServiceConfigProviderFactory providerFactory;
    private final SelfServiceFactory serviceFactory;

    private final Map<String, RequestHandler> serviceCache;
    private final String descriptorKey;
    private final ApiDescription descriptor;

    /**
     * Constructs a new self service.
     *
     * @param consoleConfigBuilderType
     *         configuration extractor
     * @param consoleConfigHandler
     *         console configuration handler
     * @param providerFactory
     *         service provider factory
     */
    @Inject
    public SelfServiceRequestHandler(Class<? extends ConsoleConfigBuilder<C>> consoleConfigBuilderType,
            ConsoleConfigHandler consoleConfigHandler, ServiceConfigProviderFactory providerFactory,
            SelfServiceFactory serviceFactory) {

        serviceCache = new ConcurrentHashMap<>();
        this.consoleConfigBuilderType = consoleConfigBuilderType;
        this.consoleConfigHandler = consoleConfigHandler;
        this.providerFactory = providerFactory;
        this.serviceFactory = serviceFactory;
        this.descriptorKey = consoleConfigBuilderType.getSimpleName();
        this.descriptor = apiDescription()
                .id("fake:id").version("fake")
                .paths(paths()
                        .put("", versionedPath()
                                .put(VersionedPath.UNVERSIONED, resource()
                                        .title(i18nString(TITLE))
                                        .description(i18nString(DESCRIPTION))
                                        .mvccSupported(false)
                                        .resourceSchema(schema()
                                                .schema(schemaFromResource("resource"))
                                                .build())
                                        .read(read().description(i18nString(READ_DESCRIPTION)).build())
                                        .action(action()
                                                .name("submitRequirements")
                                                .description(i18nString(ACTION + "submitRequirements." + DESCRIPTION))
                                                .request(schema().schema(schemaFromResource("submit.req")).build())
                                                .response(schema().schema(schemaFromResource("submit.resp")).build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        consoleConfigHandler.registerListener(this, consoleConfigBuilderType);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return getService(context).handleRead(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        } catch (RuntimeException rE) {
            logger.error("Unable to handle read", rE);
            return new InternalServerErrorException("Unable to handle read", rE).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            return getService(context).handleAction(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        } catch (RuntimeException rE) {
            logger.error("Unable to handle action", rE);
            return new InternalServerErrorException("Unable to handle action", rE).asPromise();
        }
    }

    private RequestHandler getService(Context context) throws NotSupportedException {
        String realm = RealmContext.getRealm(context).asPath().toLowerCase();
        RequestHandler service = serviceCache.get(realm);

        if (service == null) {
            synchronized (serviceCache) {
                service = serviceCache.get(realm);

                if (service == null) {
                    service = createNewService(context, realm);
                    serviceCache.put(realm, service);
                }
            }
        }

        return service;
    }

    private RequestHandler createNewService(Context context, String realm) throws NotSupportedException {
        C consoleConfig = consoleConfigHandler.getConfig(realm, consoleConfigBuilderType);
        ServiceConfigProvider<C> serviceConfigProvider = providerFactory.getProvider(consoleConfig);

        if (!serviceConfigProvider.isServiceEnabled(consoleConfig)) {
            throw new NotSupportedException("Service not configured");
        }

        return serviceFactory.getService(realm, serviceConfigProvider.getServiceConfig(consoleConfig, context, realm));
    }

    @Override
    public final void configUpdate(String source, String realm) {
        synchronized (serviceCache) {
            serviceCache.remove(realm.toLowerCase());
        }
    }

    private LocalizableString i18nString(String suffix) {
        ClassLoader loader = this.getClass().getClassLoader();
        return new LocalizableString(SELF_SERVICE_REQUEST_HANDLER + descriptorKey + "#" + suffix, loader);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return descriptor;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return descriptor;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        // Doesn't change so no need to support listeners.
    }

    @Override
    public void removeDescriptorListener(Listener listener) {

    }

    private JsonValue schemaFromResource(String schemaName) {
        return fromResource(this.getClass(), "SelfServiceRequestHandler." + schemaName + ".schema.json")
                .as(new UpdateJsonLocalizableStringRefs())
                .as(new TranslateJsonSchema(this.getClass().getClassLoader()));
    }

    private class UpdateJsonLocalizableStringRefs implements Function<JsonValue, JsonValue, NeverThrowsException> {

        /**
         * Applies the translation to string values by converting them to {@code LocalizableString}.
         * It traverses the JsonValue structure, iteratively applying the function to each item
         * in a collection.
         * @param value A JsonValue.
         * @return a transformed copy of the JsonValue input.
         */
        @Override
        public JsonValue apply(JsonValue value) {
            JsonValue returnValue = value;
            if (value.isCollection()) {
                JsonValue transformedValue = json(array());
                for (JsonValue item : value) {
                    transformedValue.add(item.as(this).getObject());
                }
                returnValue = transformedValue;
            } else if (value.isMap()) {
                JsonValue transformedValue = json(object());
                for (String key : value.keys()) {
                    transformedValue.put(key, value.get(key).as(this).getObject());
                }
                returnValue = transformedValue;
            } else if (value.isString() && value.asString().startsWith(GENERIC_I18N_KEY)) {
                returnValue = json(value.asString().replace(GENERIC_I18N_KEY,
                        SELF_SERVICE_REQUEST_HANDLER + descriptorKey + "#"));
            }
            return returnValue;
        }
    }

}
