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

package org.forgerock.openam.sts.rest.service;

import org.apache.cxf.ws.security.sts.provider.STSException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * The CREST entry point into the Rest STS
 */
public class RestSTSService implements SingletonResourceProvider {
    private static final String TRANSLATE = "translate";
    private static final String DESIRED_TOKEN_TYPE = "desiredTokenType";
    private final RestSTS restSts;
    private final Logger logger;

    /*
    Ctor not injected because this constructor is called with a specific RestSTS implementation by the RestSTSInstancePublisher
    implementation.
     */
    public RestSTSService(RestSTS restSts, Logger logger) {
        this.restSts = restSts;
        this.logger = logger;
    }

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        if (TRANSLATE.equals(request.getAction())) {
            SecurityContext securityContext = context.asContext(SecurityContext.class);
            HttpContext httpContext = context.asContext(HttpContext.class);
            String desiredTokenType = httpContext.getParameterAsString(DESIRED_TOKEN_TYPE); //TODO: in AMSTSConstants?
            if (desiredTokenType == null) {
                handler.handleError(new BadRequestException("The " + DESIRED_TOKEN_TYPE + " query parameter has not been specified."));
            }
            try {
                JsonValue result = restSts.translateToken(request.getContent(), desiredTokenType, httpContext, securityContext);
                handler.handleResult(result);
            } catch (ResourceException e) {
                /*
                This block entered for both TokenValidationException and TokenCreationException instances
                 */
                handler.handleError(e);
            } catch (AMSTSRuntimeException e) {
                /*
                RuntimeException thrown by the AM implementation of CXF-STS-defined interfaces
                 */
                logger.error("AMSTSException caught in the RestSTSService: " + e, e);
                handler.handleError(new InternalServerErrorException(e.getMessage()));
            } catch (STSException e) {
                /*
                RuntimeException thrown by the CXF-STS engine
                 */
                logger.error("Unexpected: STSException caught in the RestSTSService: " + e, e);
                handler.handleError(new InternalServerErrorException(e.getMessage()));
            }
        } else {
            handler.handleError(new BadRequestException("_action " + request.getAction() + " is not supported."));
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
