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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish.service;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

public class RestSTSPublishService implements SingletonResourceProvider {
    private static final String ADD_INSTANCE = "add_instance";
    private static final String REMOVE_INSTANCE = "remove_instance";
    private static final String REALM_PATH = "realm_path";
    private static final String RESULT = "result";

    private final RestSTSInstancePublisher publisher;
    private final Logger logger;

    public RestSTSPublishService(RestSTSInstancePublisher publisher, Logger logger) {
        this.publisher = publisher;
        this.logger = logger;
    }

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        HttpContext httpContext = context.asContext(HttpContext.class);
        String realmPath = httpContext.getParameterAsString(REALM_PATH);
        if (realmPath == null) {
            handler.handleError(new BadRequestException("The " + REALM_PATH + " query parameter has not been specified."));
        }
        final String action = request.getAction();
        if (ADD_INSTANCE.equals(action)) {
            RestSTSInstanceConfig instanceConfig = null;
            try {
                instanceConfig = RestSTSInstanceConfig.fromJson(request.getContent());
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into RestSTSInstanceConfig instance: " + e);
                handler.handleError(new BadRequestException(e));
                return;
            }
            Injector instanceInjector = null;
            try {
                instanceInjector = Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
            } catch (Exception e) {
                String message = "Exception caught creating the guice injector corresponding to rest sts instance: " + e;
                logger.error(message);
                handler.handleError(new InternalServerErrorException(message, e));
                return;
            }
            try {
                String urlElement = instanceConfig.getDeploymentConfig().getUriElement();
                publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), urlElement);
                handler.handleResult(json(object(field(RESULT, "rest sts instance successfully published at " + urlElement))));
            } catch (STSInitializationException e) {
                String message = "Exception caught publishing instance: " + e;
                logger.error(message, e);
                handler.handleError(new InternalServerErrorException(message, e));
            }
        } else if (REMOVE_INSTANCE.equals(action)) {
            publisher.removeInstance(realmPath);
            handler.handleResult(json(object(field(RESULT, "rest sts instance successfully removed from " + realmPath))));
        } else {
            handler.handleError(new BadRequestException("_action " + action + " is not supported."));
        }
    }

    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }
}
