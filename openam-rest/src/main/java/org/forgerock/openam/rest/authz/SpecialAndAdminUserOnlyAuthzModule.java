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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.openam.rest.authz;

import static org.forgerock.json.resource.ResourceException.*;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.Context;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.HttpURLConnection;

/**
 * This AuthzModule checks for the presence of the user corresponding to the token returned from
 * AccessController.doPrivileged(AdminTokenAction.getInstance()), or other special user, as evaluated by
 * AuthD#isSpecialUser. If that authZ fails, it delegates to the AdminOnlyAuthzModule. This class is subclassed by
 * the STSTokenGenerationServiceAuthzModule, which needs to provide access to special users (for rest-sts consumption),
 * to soap-sts-agent users (for soap-sts consumption), and to Admins (so that tokens can be queried and cancelled).
 */
public class SpecialAndAdminUserOnlyAuthzModule extends AdminOnlyAuthzModule {
    public static final String NAME = "SpecialUserOnlyFilter";

    protected SpecialUserIdentity specialUserIdentity;

    @Inject
    public SpecialAndAdminUserOnlyAuthzModule(Config<SessionService> sessionService, SpecialUserIdentity specialUserIdentity, @Named("frRest") Debug debug) {
        super(sessionService, debug);
        this.specialUserIdentity = specialUserIdentity;
    }

    @Override
    public String getName() {
        return NAME;
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

    @Override
    protected Promise<AuthorizationResult, ResourceException> authorize(Context context) {
        SSOTokenContext tokenContext = context.asContext(SSOTokenContext.class);
        String userId;
        try {
            SSOToken token = tokenContext.getCallerSSOToken();
            userId = token.getPrincipal().getName();
            if (specialUserIdentity.isSpecialUser(token)) {
                if (debug.messageEnabled()) {
                    debug.message("SpecialAndAdminUserOnlyAuthzModule :: User, " + userId + " accepted as Special user.");
                }
                return Promises.newResultPromise(AuthorizationResult.accessPermitted());
            } else {
                return super.authorize(context);
            }
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("SpecialAndAdminUserOnlyAuthzModule :: Unable to authorize as Special user using SSO Token.", e);
            }
            return getException(HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage(), e).asPromise();
        }
    }
}
