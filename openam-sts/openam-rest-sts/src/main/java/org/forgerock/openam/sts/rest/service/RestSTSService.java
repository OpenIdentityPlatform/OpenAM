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

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
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
    static final String TRANSLATE = "translate";
    static final String VALIDATE = "validate";
    static final String CANCEL = "cancel";
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
            return new NotSupportedException("The specified _action parameter is not supported.").asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleTranslate(Context context, ActionRequest request) {
        RestSTSTokenTranslationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenTranslationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return e.asPromise();
        }
        try {
            final JsonValue result = restSts.translateToken(invocationState, context);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for TokenMarshalException, TokenValidationException and TokenCreationException instances
             */
            logger.warn("{}: {}",invocationState ,e.toString());
            return e.asPromise();
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking translateToken: " + e, e);
            return new InternalServerErrorException(e.getMessage()).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleValidate(Context context, ActionRequest request) {
        RestSTSTokenValidationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenValidationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return e.asPromise();
        }
        try {
            final JsonValue result = restSts.validateToken(invocationState);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for both TokenValidationException and TokenMarshalException instances
             */
            logger.error("Exception caught in ValidateToken call: " + e, e);
            return e.asPromise();
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking validateToken: " + e, e);
            return new InternalServerErrorException(e.getMessage()).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleCancel(Context context, ActionRequest request) {
        RestSTSTokenCancellationInvocationState invocationState;
        try {
            invocationState = RestSTSTokenCancellationInvocationState.fromJson(request.getContent());
        } catch (TokenMarshalException e) {
            return e.asPromise();
        }
        try {
            final JsonValue result = restSts.cancelToken(invocationState);
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException e) {
            /*
            This block entered for both TokenValidationException and TokenMarshalException instances
             */
            logger.error("Exception caught in CancelToken call: " + e, e);
            return e.asPromise();
        } catch (Exception e) {
            logger.error("Unexpected: Exception caught in the RestSTSService invoking cancelToken: " + e, e);
            return new InternalServerErrorException(e.getMessage()).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException().asPromise();
    }

    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }
}
