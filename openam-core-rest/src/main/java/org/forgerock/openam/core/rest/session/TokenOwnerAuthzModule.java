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
 */

package org.forgerock.openam.core.rest.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
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
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Verifies that the token for interrogation is the same token as the
 * sender of the request, or that the sender of the request is an administrator.
 *
 * If no tokenId is provided by the request, this module will assume that the
 * resource will identify the caller by the Context, and return true.
 *
 * If a tokenId is provided and invalid, this module will return false.
 *
 * If a tokenId is provided and the universal id of that token owner is equal to the
 * universal id of the token provided by the context of the request, this module will
 * return true.
 *
 * In all other circumstances this module will return a failure.
 */
public class TokenOwnerAuthzModule implements CrestAuthorizationModule {

    public final static String NAME = "TokenOwnerAuthzModuleFilter";

    private final SSOTokenManager ssoTokenManager;
    private final Set<String> allowedActions;
    private final String tokenId;

    /**
     * Creates an authz module that will verify that a tokenId provided by the user (via query params)
     * is the same user (via universal identifier) as the user requesting the action.
     *
     * @param tokenId The tokenId query parameter. May not be null.
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param allowedActions A list of allowed actions. Will be matched ignoring case.
     */
    public TokenOwnerAuthzModule(String tokenId, SSOTokenManager ssoTokenManager, String... allowedActions) {
        Reject.ifNull(allowedActions);
        Reject.ifTrue(StringUtils.isEmpty(tokenId));

        this.ssoTokenManager = ssoTokenManager;
        this.allowedActions = new HashSet<>(Arrays.asList(allowedActions));
        this.tokenId = tokenId;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(Context context, CreateRequest createRequest) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(Context context, ReadRequest readRequest) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(Context context, UpdateRequest updateRequest) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(Context context, DeleteRequest deleteRequest) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(Context context, PatchRequest patchRequest) {
        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(Context context, ActionRequest request) {

        try {
            if (actionCanBeInvokedBySelf(request.getAction()) && isTokenOwner(context, request)) {
                return Promises.newResultPromise(AuthorizationResult.accessPermitted());
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (SSOException e) {
            return new ForbiddenException().asPromise();
        }

        return new ForbiddenException().asPromise();
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(Context context, QueryRequest queryRequest) {
        return new ForbiddenException().asPromise();
    }

    private boolean isTokenOwner(Context context, ActionRequest request)
            throws ResourceException, SSOException {
        String loggedInUserId = getUserId(context);

        final String queryTemp = request.getAdditionalParameter(tokenId);
        if (StringUtils.isEmpty(queryTemp)) { //if there's no tokenId then we're talking about ourselves
            return true;
        }

        final String queryUsername = ssoTokenManager.createSSOToken(queryTemp).getProperty(Constants.UNIVERSAL_IDENTIFIER);
        return queryUsername.equalsIgnoreCase(loggedInUserId);
    }

    private boolean actionCanBeInvokedBySelf(String actionId) {
        for (String allowed : allowedActions) {
            if (actionId.equalsIgnoreCase(allowed)) {
                return true;
            }
        }

        return false;
    }

    private String getUserId(Context context) throws ResourceException {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);

        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            return token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            throw new ForbiddenException(e.getMessage(), e);
        }
    }
}
