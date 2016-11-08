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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.authz;

import static org.forgerock.openam.utils.CollectionUtils.transformSet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.authz.PrivilegeDefinition.Action;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;

/**
 * This authorisation module ties the calling subject back into the delegation privilege framework to
 * verify whether the subject has been delegated the privilege to carry out the action they're attempting.
 *
 * @since 12.0.0
 */
public abstract class PrivilegeAuthzModule {

    public static final String NAME = "DelegationFilter";
    public static final PrivilegeDefinition MODIFY = PrivilegeDefinition.getInstance("modify", Action.MODIFY);
    public static final PrivilegeDefinition READ = PrivilegeDefinition.getInstance("read", Action.READ);

    private static final ActionToStringMapper ACTION_TO_STRING_MAPPER = new ActionToStringMapper();
    private static final String REST = "rest";
    private static final String VERSION = "1.0";

    private final DelegationEvaluator evaluator;
    private final DelegationPermissionFactory permissionFactory;
    private final CoreWrapper coreWrapper;
    private final SSOTokenManager ssoTokenManager;

    protected final Map<String, PrivilegeDefinition> actionToDefinition;

    /**
     * Create a new instance of {@link PrivilegeAuthzModule}.
     *
     * @param evaluator The Delegation Evaluator.
     * @param actionToDefinition The action to definition map.
     * @param permissionFactory The Delegation Permission Factory.
     * @param coreWrapper The Core Wrapper.
     * @param ssoTokenManager The SSOToken manager.
     */
    public PrivilegeAuthzModule(DelegationEvaluator evaluator, Map<String, PrivilegeDefinition> actionToDefinition,
            DelegationPermissionFactory permissionFactory, CoreWrapper coreWrapper, SSOTokenManager ssoTokenManager) {
        this.evaluator = evaluator;
        this.actionToDefinition = actionToDefinition;
        this.permissionFactory = permissionFactory;
        this.coreWrapper = coreWrapper;
        this.ssoTokenManager = ssoTokenManager;
    }

    /**
     * Given the calling context and the privilege definition attempts to authorise the calling subject.
     *
     * @param context
     *         the server context
     * @param definition
     *         the privilege definition
     *
     * @return the authorisation result
     */
    protected AuthorizationResult evaluate(Context context, PrivilegeDefinition definition)
            throws InternalServerErrorException {

        // If no realm is specified default to the root realm.
        final String realm = (context.containsContext(RealmContext.class)) ?
                context.asContext(RealmContext.class).getRealm().asPath() : "/";
        final SubjectContext subjectContext = context.asContext(SubjectContext.class);
        final UriRouterContext routerContext = context.asContext(UriRouterContext.class);

        // Map the set of actions to a set of action strings.
        final Set<String> actions = transformSet(definition.getActions(), ACTION_TO_STRING_MAPPER);

        try {
            Session callerSession = subjectContext.getCallerSession();
            if (callerSession == null) {
                // you don't have a session so return access denied
                return AuthorizationResult.accessDenied("No session for request.");
            }

            // check session status using SSOTokenManager and refresh if necessary
            SSOToken token = subjectContext.getCallerSSOToken();
            if (!ssoTokenManager.isValidToken(token)) {
                return AuthorizationResult.accessDenied("No valid session in request.");
            }

            final String loggedInRealm =
                    coreWrapper.convertOrgNameToRealmName(callerSession.getClientDomain());

            final DelegationPermission permissionRequest = permissionFactory.newInstance(
                    loggedInRealm, REST, VERSION, routerContext.getMatchedUri(),
                    definition.getCommonVerb(), actions, Collections.<String, String>emptyMap());

            // If the subject is an agent, the realm in the context and the realm of the subject do not have to be
            // either the same, or related by parentage
            //
            boolean isRealmValid = IdentityUtils.isCASPAorJASPA(token) || loggedIntoValidRealm(realm, loggedInRealm);

            if (evaluator.isAllowed(subjectContext.getCallerSSOToken(), permissionRequest,
                    Collections.<String, Set<String>>emptyMap()) && isRealmValid) {
                // Authorisation has been approved.
                return AuthorizationResult.accessPermitted();
            }
        } catch (DelegationException dE) {
            throw new InternalServerErrorException("Attempt to authorise the user has failed", dE);
        } catch (SSOException e) {
            // you don't have a user so return access denied
            return AuthorizationResult.accessDenied("No valid user supplied in request.");
        }

        return AuthorizationResult.accessDenied("The user has insufficient privileges");
    }

    /**
     * Check to see if the realm logged into is valid for getting access to the realm requested.
     * @param requestedRealm The realm requested.
     * @param loggedInRealm The realm logged in to.
     * @return
     */
    protected boolean loggedIntoValidRealm(String requestedRealm, String loggedInRealm) {
        return requestedRealm.equalsIgnoreCase(loggedInRealm)
                    || RealmUtils.isParentRealm(loggedInRealm, requestedRealm);
    }

    /**
     * Function converts an action to a string representation.
     */
    private final static class ActionToStringMapper implements Function<Action, String, NeverThrowsException> {

        @Override
        public String apply(final Action action) {
            return action.toString();
        }
    }

}
