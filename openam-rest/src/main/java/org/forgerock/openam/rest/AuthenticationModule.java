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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestVisitor;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 *
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

    public AuthenticationModule(AsyncServerAuthModule ssoTokenAuthModule,
            AsyncServerAuthModule optionalSsoTokenAuthModule) {
        this.ssoTokenAuthModule = ssoTokenAuthModule;
        this.optionalSsoTokenAuthModule = optionalSsoTokenAuthModule;
    }

    public void exceptCreate() {
        exceptCreate = true;
    }

    public void exceptRead() {
        exceptRead = true;
    }

    public void exceptUpdate() {
        exceptUpdate = true;
    }

    public void exceptDelete() {
        exceptDelete = true;
    }

    public void exceptPatch() {
        exceptPatch = true;
    }

    public void exceptActions(String... actions) {
        exceptActions.addAll(Arrays.asList(actions));
    }

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
        return ssoTokenAuthModule.initialize(requestPolicy, responsePolicy, callbackHandler, settings);
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

        Request request = messageInfo.getRequest();
        if (request.accept(new AuthenticateRequestVisitor(), null)) {
            DEBUG.message(request.getClass().getName() + " for resource, " + request.getResourcePath()
                    + " is a protected resource.");
            return ssoTokenAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
        } else {
            DEBUG.message(request.getClass().getName() + " for resource, " + request.getResourcePath()
                    + " is not a protected resource.");
            return optionalSsoTokenAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
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

    private final class AuthenticateRequestVisitor implements RequestVisitor<Boolean, Void> {

        @Override
        public Boolean visitActionRequest(Void aVoid, ActionRequest request) {
            return !exceptActions.contains(request.getAction());
        }

        @Override
        public Boolean visitCreateRequest(Void aVoid, CreateRequest request) {
            return !exceptCreate;
        }

        @Override
        public Boolean visitDeleteRequest(Void aVoid, DeleteRequest request) {
            return !exceptDelete;
        }

        @Override
        public Boolean visitPatchRequest(Void aVoid, PatchRequest request) {
            return !exceptPatch;
        }

        @Override
        public Boolean visitQueryRequest(Void aVoid, QueryRequest request) {
            return !exceptQuery;
        }

        @Override
        public Boolean visitReadRequest(Void aVoid, ReadRequest request) {
            return !exceptRead;
        }

        @Override
        public Boolean visitUpdateRequest(Void aVoid, UpdateRequest request) {
            return !exceptUpdate;
        }
    }
}
