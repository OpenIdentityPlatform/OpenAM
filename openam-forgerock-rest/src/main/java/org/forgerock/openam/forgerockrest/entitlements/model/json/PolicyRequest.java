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

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.PolicyEvaluator;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic policy request that captures the common attributes for all policy requests.
 *
 * @since 12.0.0
 */
public abstract class PolicyRequest {

    // Used to map a list to a set.
    private final static ListToSetMapper LIST_TO_SET_MAPPER = new ListToSetMapper();

    private final Subject restSubject;
    private final Subject policySubject;
    private final String application;
    private final String realm;
    private final Map<String, Set<String>> environment;

    PolicyRequest(final PolicyRequestBuilder<?> builder) {
        restSubject = builder.restSubject;
        policySubject = builder.policySubject;
        application = builder.application;
        realm = builder.realm;
        environment = builder.environment;
    }

    public Subject getRestSubject() {
        return restSubject;
    }

    public Subject getPolicySubject() {
        return policySubject;
    }

    public String getApplication() {
        return application;
    }

    public String getRealm() {
        return realm;
    }

    public Map<String, Set<String>> getEnvironment() {
        return environment;
    }

    /**
     * Given the policy evaluator dispatch oneself as one knows best.
     *
     * @param evaluator
     *         the non-null policy evaluator
     *
     * @return a list of policy decisions retrieved from the evaluator
     *
     * @throws EntitlementException
     *         should dispatch and evaluation fail
     */
    public abstract List<Entitlement> dispatch(PolicyEvaluator evaluator) throws EntitlementException;

    /**
     * Policy request builder used to assist with the construction of policy requests and to bring some separation.
     *
     * @param <T>
     *         the concrete request type this builder makes
     */
    static abstract class PolicyRequestBuilder<T extends PolicyRequest> {

        private final static String ROOT_REALM = "/";
        private final static String APPLICATION = "application";
        private final static String ENVIRONMENT = "environment";
        private final static String SUBJECT = "subject";

        private final Subject restSubject;
        private final Subject policySubject;
        private final String application;
        private final String realm;
        private final Map<String, Set<String>> environment;

        /**
         * Standard builder constructor.
         *
         * @param context
         *         non-null context
         * @param request
         *         non-null request
         *
         * @throws EntitlementException
         *         should the request construction fail
         */
        PolicyRequestBuilder(final ServerContext context, final ActionRequest request) throws EntitlementException {
            Reject.ifNull(context, request);

            final SubjectContext subjectContext = context.asContext(SubjectContext.class);
            final RealmContext realmContext = context.asContext(RealmContext.class);
            Reject.ifNull(subjectContext, realmContext);

            restSubject = getRestSubject(subjectContext);

            final JsonValue jsonValue = request.getContent();
            Reject.ifNull(jsonValue);

            policySubject = getPolicySubject(subjectContext, jsonValue, restSubject);
            application = getApplication(jsonValue);
            realm = getRealm(realmContext);
            environment = getEnvironment(jsonValue);
        }

        private Subject getRestSubject(final SubjectContext context) throws EntitlementException {
            final Subject restSubject = context.getCallerSubject();

            if (restSubject == null) {
                // Caller of the REST service is required to have been authenticated.
                throw new EntitlementException(EntitlementException.PERMISSION_DENIED);
            }

            return restSubject;
        }

        private Subject getPolicySubject(final SubjectContext context, final JsonValue value,
                                         final Subject defaultSubject) throws EntitlementException {
            if (value.isDefined(SUBJECT)) {
                final String tokenId = value.get(SUBJECT).asString();
                final Subject policySubject = context.getSubject(tokenId);

                if (policySubject == null) {
                    // Invalid subject defined.
                    throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[]{SUBJECT});
                }

                return policySubject;
            }

            // If no subject has been specified default to the REST subject.
            return defaultSubject;
        }

        private String getApplication(final JsonValue value) {
            return value.get(APPLICATION).defaultTo(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME).asString();
        }

        private String getRealm(final RealmContext context) {
            return StringUtils.ifNullOrEmpty(context.getRealm(), ROOT_REALM);
        }

        private Map<String, Set<String>> getEnvironment(final JsonValue value) {
            final Map<String, List<String>> environment = value.get(ENVIRONMENT).asMapOfList(String.class);
            return (environment != null) ?
                    CollectionUtils.transformMap(environment, LIST_TO_SET_MAPPER) :
                    new HashMap<String, Set<String>>();
        }

        /**
         * @return a concrete policy request instance
         */
        abstract T build();

    }

    /**
     * Mapper function used to transform a list of strings to a set of strings.
     */
    private final static class ListToSetMapper implements Function<List<String>, Set<String>, NeverThrowsException> {

        @Override
        public Set<String> apply(final List<String> value) {
            return new HashSet<String>(value);
        }

    }

}
