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

package org.forgerock.openam.rest;

import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpUtils;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * CAF authentication module implementation for protecting CREST endpoints that
 * allows for exceptions to be added for certain CREST operations.
 *
 * @since 13.0.0
 */
public class AuthenticationModule implements AsyncServerAuthModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private final AsyncServerAuthModule ssoTokenAuthModule;
    private final AsyncServerAuthModule optionalSsoTokenAuthModule;

    private boolean exceptCreate = false;
    private boolean exceptRead = false;
    private boolean exceptUpdate = false;
    private boolean exceptDelete = false;
    private boolean exceptPatch = false;
    private List<String> exceptActions = new ArrayList<>();
    private boolean exceptQuery = false;

    AuthenticationModule(AsyncServerAuthModule ssoTokenAuthModule,
            AsyncServerAuthModule optionalSsoTokenAuthModule) {
        this.ssoTokenAuthModule = ssoTokenAuthModule;
        this.optionalSsoTokenAuthModule = optionalSsoTokenAuthModule;
    }

    /**
     * Marks authentication on create requests to the route as optional.
     */
    public void exceptCreate() {
        exceptCreate = true;
    }

    /**
     * Marks authentication on read requests to the route as optional.
     */
    public void exceptRead() {
        exceptRead = true;
    }

    /**
     * Marks authentication on update requests to the route as optional.
     */
    public void exceptUpdate() {
        exceptUpdate = true;
    }

    /**
     * Marks authentication on delete requests to the route as optional.
     */
    public void exceptDelete() {
        exceptDelete = true;
    }

    /**
     * Marks authentication on patch requests to the route as optional.
     */
    public void exceptPatch() {
        exceptPatch = true;
    }

    /**
     * Marks authentication on action requests, with the specified action ids,
     * to the route as optional.
     *
     * @param actions The excluded actions.
     */
    public void exceptActions(String... actions) {
        exceptActions.addAll(Arrays.asList(actions));
    }

    /**
     * Marks authentication on query requests to the route as optional.
     */
    public void exceptQuery() {
        exceptQuery = true;
    }

    @Override
    public String getModuleId() {
        return ssoTokenAuthModule.getModuleId();
    }

    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy,
            MessagePolicy responsePolicy, CallbackHandler callbackHandler, Map<String, Object> settings) {
        List<Promise<Void, AuthenticationException>> promises = new ArrayList<>();
        promises.add(ssoTokenAuthModule.initialize(requestPolicy, responsePolicy, callbackHandler, settings));
        promises.add(optionalSsoTokenAuthModule.initialize(requestPolicy, responsePolicy, callbackHandler, settings));
        return Promises.when(promises)
                .thenAsync(new AsyncFunction<List<Void>, Void, AuthenticationException>() {
                    @Override
                    public Promise<Void, AuthenticationException> apply(List<Void> value) {
                        return newResultPromise(null);
                    }
                });
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        Collection<Class<?>> supportedMessageTypes = new HashSet<>();
        supportedMessageTypes.addAll(ssoTokenAuthModule.getSupportedMessageTypes());
        supportedMessageTypes.retainAll(optionalSsoTokenAuthModule.getSupportedMessageTypes());
        return supportedMessageTypes;
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        boolean optional = false;

        Request request = messageInfo.getRequest();
        try {
            switch (HttpUtils.determineRequestOperation(request)) {
                case CREATE: {
                    optional = exceptCreate;
                    break;
                }
                case READ: {
                    optional = exceptRead;
                    break;
                }
                case UPDATE: {
                    optional = exceptUpdate;
                    break;
                }
                case DELETE: {
                    optional = exceptDelete;
                    break;
                }
                case PATCH: {
                    optional = exceptPatch;
                    break;
                }
                case ACTION: {
                    optional = exceptActions.contains(request.getForm().getFirst(HttpUtils.PARAM_ACTION));
                    break;
                }
                case QUERY: {
                    optional = exceptQuery;
                    break;
                }
            }

            if (!optional) {
                DEBUG.message(request.getClass().getName() + " for resource, "
                        + messageInfo.asContext(UriRouterContext.class).getRemainingUri()
                        + " is a protected resource.");
                return ssoTokenAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            } else {
                DEBUG.message(request.getClass().getName() + " for resource, "
                        + messageInfo.asContext(UriRouterContext.class).getRemainingUri()
                        + " is not a protected resource.");
                return optionalSsoTokenAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            }
        } catch (ResourceException e) {
            return newExceptionPromise(new AuthenticationException("Failed to authenticate request", e));
        }
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        return ssoTokenAuthModule.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo,
            Subject clientSubject) {
        return ssoTokenAuthModule.cleanSubject(messageInfo, clientSubject);
    }
}
