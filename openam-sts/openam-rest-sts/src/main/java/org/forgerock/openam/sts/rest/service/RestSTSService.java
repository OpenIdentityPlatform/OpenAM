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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.service;

import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenCancellationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenTranslationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenValidationInvocationState;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;

/**
 * The CREST entry point into the Rest STS
 */
public class RestSTSService implements SingletonResourceProvider {
    private static final String TRANSLATE = "translate";
    private static final String VALIDATE = "validate";
    private static final String CANCEL = "cancel";
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

    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        switch (request.getAction()) {
            case TRANSLATE:
                return handleTranslate(context, request);
            case VALIDATE:
                return handleValidate(context, request);
            case CANCEL:
                return handleCancel(context, request);
            default:
                return newExceptionPromise(newNotSupportedException("The specified _action parameter is not supported."));
        }
    }

    private Promise<ActionResponse, ResourceException> handleTranslate(Context context, ActionRequest request) {
        RestSTSServiceHttpServletContext servletContext = context.asContext(RestSTSServiceHttpServletContext.class);
        HttpContext httpContext = context.asContext(HttpContext.class);
        RestSTSTokenTranslationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenTranslationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return newExceptionPromise(adapt(e));
        }
        try {
            final JsonValue result = restSts.translateToken(invocationState, httpContext, servletContext);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for TokenMarshalException, TokenValidationException and TokenCreationException instances
             */
            logger.error("Exception caught in translateToken call: " + e, e);
            return newExceptionPromise(e);
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking translateToken: " + e, e);
            return newExceptionPromise(newInternalServerErrorException(e.getMessage()));
        }
    }

    private Promise<ActionResponse, ResourceException> handleValidate(Context context, ActionRequest request) {
        RestSTSTokenValidationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenValidationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return newExceptionPromise(adapt(e));
        }
        try {
            final JsonValue result = restSts.validateToken(invocationState);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for both TokenValidationException and TokenMarshalException instances
             */
            logger.error("Exception caught in ValidateToken call: " + e, e);
            return newExceptionPromise(e);
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking validateToken: " + e, e);
            return newExceptionPromise(newInternalServerErrorException(e.getMessage()));
        }
    }

    private Promise<ActionResponse, ResourceException> handleCancel(Context context, ActionRequest request) {
        RestSTSTokenCancellationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenCancellationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return newExceptionPromise(adapt(e));
        }
        try {
            final JsonValue result = restSts.cancelToken(invocationState);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for both TokenValidationException and TokenMarshalException instances
             */
            logger.error("Exception caught in CancelToken call: " + e, e);
            return newExceptionPromise(e);
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking cancelToken: " + e, e);
            return newExceptionPromise(newInternalServerErrorException(e.getMessage()));
        }
    }

    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }
}
