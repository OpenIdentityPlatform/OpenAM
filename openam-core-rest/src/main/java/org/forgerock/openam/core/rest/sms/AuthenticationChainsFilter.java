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

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;

/**
 * Authentication Chain endpoint filter which converts the
 * authChainConfiguration from a XML String into a JSON Array.
 *
 * <p>If the options String is parsable as a key value pair separated by commas
 * then it will be parsed into a Map otherwise it will be returned as a String.
 * </p>
 */
public class AuthenticationChainsFilter implements Filter {

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        return next.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        try {
            return wrap(next.handleCreate(context, transformRequest(request)));
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        return wrap(next.handleDelete(context, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        //Not currently handling converting authChainConfiguration for patch
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        return next.handleQuery(context, request, wrap(handler));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        return wrap(next.handleRead(context, request));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        try {
            return wrap(next.handleUpdate(context, transformRequest(request)));
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

    private CreateRequest transformRequest(CreateRequest request) throws InternalServerErrorException {
        request.setContent(transformRequestBody(request.getContent()));
        return request;
    }

    private UpdateRequest transformRequest(UpdateRequest request) throws InternalServerErrorException {
        request.setContent(transformRequestBody(request.getContent()));
        return request;
    }

    private JsonValue transformRequestBody(JsonValue body) throws InternalServerErrorException {
        if (body.isDefined("authChainConfiguration")) {
            try {
                List<AuthConfigurationEntry> entries = new ArrayList<>();
                for (JsonValue entry : body.get("authChainConfiguration")) {
                    String module = entry.get("module").asString();
                    String criteria = entry.get("criteria").asString();
                    String options = getOptions(entry);
                    entries.add(new AuthConfigurationEntry(module, criteria, options));
                }
                body.put("authChainConfiguration", AMAuthConfigUtils.authConfigurationEntryToXMLString(entries));
            } catch (AMConfigurationException e) {
                throw new InternalServerErrorException("Failed to parse authChainConfiguration", e);
            }
        }
        return body;
    }

    private String getOptions(JsonValue entry) {
        JsonValue options = entry.get("options");
        if (options.isNull()) {
            return "";
        } else if (options.isString()) {
            return options.asString();
        } else if (options.isMap() && !options.asMap().isEmpty()) {
            StringBuilder optionsBuilder = new StringBuilder();
            for (Map.Entry<String, String> option : options.asMap(String.class).entrySet()) {
                optionsBuilder.append(option.getKey()).append("=").append(option.getValue()).append(",");
            }
            return optionsBuilder.substring(0, optionsBuilder.length() - 1);
        }
        return "";
    }

    private QueryResourceHandler wrap(final QueryResourceHandler handler) {
        return new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                return handler.handleResource(transformAuthChainConfiguration(resource));
            }
        };
    }

    private Promise<ResourceResponse, ResourceException> wrap(Promise<ResourceResponse, ResourceException> promise) {
        return promise
                .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(ResourceResponse response) {
                        return newResultPromise(transformAuthChainConfiguration(response));
                    }
                });
    }

    private ResourceResponse transformAuthChainConfiguration(ResourceResponse resource) {
        if (resource.getContent().isDefined("authChainConfiguration")) {
            List<AuthConfigurationEntry> entries = AMAuthConfigUtils.xmlToAuthConfigurationEntry(
                    resource.getContent().get("authChainConfiguration").asString());
            JsonValue authChainConfiguration = json(array());
            for (AuthConfigurationEntry entry : entries) {
                JsonValue authChainEntry = json(object());
                authChainEntry.add("module", entry.getLoginModuleName());
                authChainEntry.add("criteria", entry.getControlFlag());
                authChainEntry.add("options", parseOptions(entry.getOptions()).getObject());
                authChainConfiguration.add(authChainEntry.getObject());
            }
            resource.getContent().put("authChainConfiguration", authChainConfiguration.getObject());
        }
        return resource;
    }

    private static final Pattern KEY_VALUE_PAIR_REGEX = Pattern.compile("(\\w+\\s*=\\s*[\\w\\s]+,?)+");

    private JsonValue parseOptions(String options) {
        if (options == null || options.isEmpty()) {
            return json(object());
        }
        if (KEY_VALUE_PAIR_REGEX.matcher(options).matches()) {
            JsonValue optionsValue = json(object());
            for (String pair : options.split(",")) {
                String[] keyValue = pair.trim().split("=");
                if (keyValue.length != 2) {
                    return json(options);
                }
                optionsValue.add(keyValue[0], keyValue[1]);
            }
            return optionsValue;
        } else {
            return json(options);
        }
    }
}
