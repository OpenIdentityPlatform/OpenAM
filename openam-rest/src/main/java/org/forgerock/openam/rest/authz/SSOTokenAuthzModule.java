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

package org.forgerock.openam.rest.authz;

import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Named;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

public abstract class SSOTokenAuthzModule implements CrestAuthorizationModule {
    protected final Debug debug;
    protected final String moduleName = this.getClass().getSimpleName();

    public SSOTokenAuthzModule(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context, CreateRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context, DeleteRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {
        return authorize(context);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest request) {
        return authorize(context);
    }

    protected Promise<AuthorizationResult, ResourceException> authorize(Context context) {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            return validateToken(context, token);
        } catch (SSOException e) {
            debug.message("{} :: Unable to authorize user using SSO Token.", moduleName, e);
            return newResultPromise(AuthorizationResult.accessDenied("Not authorized."));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Validate the caller's SSO Token.
     * @param context The request context.
     * @param token The caller's SSOToken.
     * @return The result promise.
     * @throws SSOException In the case of failed operations on the token. Will be converted to a forbidden result.
     * @throws ResourceException Other resource exceptions can be thrown and will be returned as the result.
     */
    protected abstract Promise<AuthorizationResult, ResourceException> validateToken(Context context, SSOToken token)
            throws SSOException, ResourceException;
}
