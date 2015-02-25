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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.authz;

import com.iplanet.sso.SSOException;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.utils.CollectionUtils.transformSet;

/**
 * This authorisation module ties the calling subject back into the delegation privilege framework to
 * verify whether the subject has been delegated the privilege to carry out the action they're attempting.
 *
 * @since 12.0.0
 */
public class PrivilegeAuthzModule implements CrestAuthorizationModule {

    public static final String NAME = "DelegationFilter";

    private static final PrivilegeDefinition MODIFY = PrivilegeDefinition
            .getInstance("modify", PrivilegeDefinition.Action.MODIFY);
    private static final PrivilegeDefinition READ = PrivilegeDefinition
            .getInstance("read", PrivilegeDefinition.Action.READ);

    private static final ActionToStringMapper ACTION_TO_STRING_MAPPER = new ActionToStringMapper();

    private static final String REST = "rest";
    private static final String VERSION = "1.0";

    private final DelegationEvaluator evaluator;
    private final Map<String, PrivilegeDefinition> actionToDefinition;
    private final DelegationPermissionFactory permissionFactory;

    @Inject
    public PrivilegeAuthzModule(final DelegationEvaluator evaluator,
                                final Map<String, PrivilegeDefinition> actionToDefinition,
                                final DelegationPermissionFactory permissionFactory) {
        this.evaluator = evaluator;
        this.actionToDefinition = actionToDefinition;
        this.permissionFactory = permissionFactory;
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(
            ServerContext serverContext, ReadRequest readRequest) {
        return evaluate(serverContext, READ);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(
            ServerContext serverContext, QueryRequest queryRequest) {
        return evaluate(serverContext, READ);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(
            ServerContext serverContext, CreateRequest createRequest) {
        return evaluate(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(
            ServerContext serverContext, UpdateRequest updateRequest) {
        return evaluate(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(
            ServerContext serverContext, DeleteRequest deleteRequest) {
        return evaluate(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(
            ServerContext serverContext, PatchRequest patchRequest) {
        return evaluate(serverContext, MODIFY);
    }

    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(
            ServerContext serverContext, ActionRequest actionRequest) {

        // Get the privilege definition for the CREST action.
        final String crestAction = actionRequest.getAction();
        final PrivilegeDefinition definition = actionToDefinition.get(crestAction);

        if (definition == null) {
            return Promises.newSuccessfulPromise(
                    AuthorizationResult.failure("No privilege mapping for requested action " + crestAction));
        }

        return evaluate(serverContext, definition);
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
    private Promise<AuthorizationResult, ResourceException> evaluate(final ServerContext context,
                                                                     final PrivilegeDefinition definition) {

        // If no realm is specified default to the root realm.
        final String realm = (context.containsContext(RealmContext.class)) ?
                context.asContext(RealmContext.class).getResolvedRealm() : "/";
        final SubjectContext subjectContext = context.asContext(SubjectContext.class);
        final RouterContext routerContext = context.asContext(RouterContext.class);

        // Map the set of actions to a set of action strings.
        final Set<String> actions = transformSet(definition.getActions(), ACTION_TO_STRING_MAPPER);

        try {
            final DelegationPermission permissionRequest = permissionFactory.newInstance(
                    realm, REST, VERSION, routerContext.getMatchedUri(),
                    definition.getCommonVerb(), actions, Collections.<String, String>emptyMap());

            if (evaluator.isAllowed(subjectContext.getCallerSSOToken(), permissionRequest,
                    Collections.<String, Set<String>>emptyMap())) {

                // Authorisation has been approved.
                return Promises.newSuccessfulPromise(AuthorizationResult.success());
            }
        } catch (DelegationException dE) {
            return Promises.newFailedPromise(
                    ResourceException.getException(500, "Attempt to authorise the user has failed", dE));
        } catch (SSOException ssoE) {
            return Promises.newFailedPromise(
                    ResourceException.getException(500, "Attempt to authorise the user has failed", ssoE));
        }

        return Promises.newSuccessfulPromise(AuthorizationResult.failure("The user has insufficient privileges"));
    }


    /**
     * Function converts an action to a string representation.
     */
    private final static class ActionToStringMapper implements
            Function<PrivilegeDefinition.Action, String, NeverThrowsException> {

        @Override
        public String apply(final PrivilegeDefinition.Action action) {
            return action.toString();
        }

    }

}
