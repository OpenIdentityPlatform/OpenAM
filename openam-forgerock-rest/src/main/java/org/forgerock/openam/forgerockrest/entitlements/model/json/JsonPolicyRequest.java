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
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
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
 * Embodies all the properties that make up a policy request.
 *
 * @since 12.0.0
 */
public final class JsonPolicyRequest {

    private final static ListToSetMapper LIST_TO_SET_MAPPER = new ListToSetMapper();

    private final static String ROOT_REALM = "/";
    private final static String RESOURCES = "resources";
    private final static String APPLICATION = "application";
    private final static String ENVIRONMENT = "environment";
    private final static String SUBJECT = "subject";

    private final Subject restSubject;
    private final Subject policySubject;
    private final Set<String> resources;
    private final String application;
    private final String realm;
    private final Map<String, Set<String>> environment;

    private JsonPolicyRequest(final Builder builder) {
        restSubject = builder.restSubject;
        policySubject = builder.policySubject;
        resources = builder.resources;
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

    public Set<String> getResources() {
        return resources;
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
     * Given the CREST context and request, draws out the
     * required properties and builds a policy request from it.
     */
    public static final class Builder {

        private Subject restSubject;
        private Subject policySubject;
        private Set<String> resources;
        private String application;
        private String realm;
        private Map<String, Set<String>> environment;

        public Builder(final ServerContext context, final ActionRequest request) throws EntitlementException {
            Reject.ifNull(context, request);

            final SubjectContext subjectContext = context.asContext(SubjectContext.class);
            final RealmContext realmContext = context.asContext(RealmContext.class);
            setRestSubject(subjectContext);
            setRealm(realmContext);

            final JsonValue jsonValue = request.getContent();
            setPolicySubject(jsonValue, subjectContext);
            setResources(jsonValue);
            setApplication(jsonValue);
            setEnvironment(jsonValue);
        }

        private void setRestSubject(final SubjectContext context) throws EntitlementException {
            final Subject restSubject = context.getCallerSubject();

            if (restSubject == null) {
                // Caller of the REST service is required to have been authenticated.
                throw new EntitlementException(EntitlementException.PERMISSION_DENIED);
            }

            this.restSubject = restSubject;
        }

        private void setPolicySubject(final JsonValue value, final SubjectContext context) throws EntitlementException {
            if (value.isDefined(SUBJECT)) {
                final String tokenId = value.get(SUBJECT).asString();
                final Subject policySubject = context.getSubject(tokenId);

                if (policySubject == null) {
                    // Invalid subject defined.
                    throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[]{SUBJECT});
                }

                this.policySubject = policySubject;
            } else {
                // If no subject has been specified default to the REST subject.
                this.policySubject = restSubject;
            }
        }

        private void setResources(final JsonValue value) throws EntitlementException {
            final List<String> resources = value.get(RESOURCES).asList(String.class);

            if (resources == null || resources.isEmpty()) {
                // Protected resources are required.
                throw new EntitlementException(EntitlementException.INVALID_VALUE, new Object[]{RESOURCES});
            }

            this.resources = new HashSet<String>(resources);
        }

        private void setRealm(final RealmContext context) {
            this.realm = StringUtils.ifNullOrEmpty(context.getRealm(), ROOT_REALM);
        }

        private void setApplication(final JsonValue value) {
            application = value.get(APPLICATION)
                    .defaultTo(ApplicationTypeManager.URL_APPLICATION_TYPE_NAME).asString();
        }

        private void setEnvironment(final JsonValue value) {
            final Map<String, List<String>> environment = value.get(ENVIRONMENT).asMapOfList(String.class);
            this.environment = (environment != null) ?
                    CollectionUtils.transformMap(environment, LIST_TO_SET_MAPPER) : new HashMap<String, Set<String>>();
        }

        public JsonPolicyRequest build() {
            return new JsonPolicyRequest(this);
        }

    }

    /**
     * Mapper function used to transform a list of strings too a set of strings.
     */
    private final static class ListToSetMapper implements Function<List<String>, Set<String>, NeverThrowsException> {

        @Override
        public Set<String> apply(final List<String> value) {
            return new HashSet<String>(value);
        }

    }

}
