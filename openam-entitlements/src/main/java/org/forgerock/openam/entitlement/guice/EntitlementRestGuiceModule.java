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

package org.forgerock.openam.entitlement.guice;

import static org.forgerock.openam.entitlement.rest.query.AttributeType.STRING;
import static org.forgerock.openam.entitlement.rest.query.AttributeType.TIMESTAMP;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.PolicyPrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.entitlement.rest.ApplicationsResource;
import org.forgerock.openam.entitlement.rest.EntitlementEvaluatorFactory;
import org.forgerock.openam.entitlement.rest.EntitlementsExceptionMappingHandler;
import org.forgerock.openam.entitlement.rest.JsonPolicyParser;
import org.forgerock.openam.entitlement.rest.PolicyEvaluatorFactory;
import org.forgerock.openam.entitlement.rest.PolicyParser;
import org.forgerock.openam.entitlement.rest.PolicyResource;
import org.forgerock.openam.entitlement.rest.PolicyStoreProvider;
import org.forgerock.openam.entitlement.rest.PrivilegePolicyStoreProvider;
import org.forgerock.openam.entitlement.rest.XacmlRouterProvider;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.service.DefaultPrivilegeManagerFactory;
import org.forgerock.openam.entitlement.service.PrivilegeManagerFactory;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.restlet.routing.Router;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Guice module for binding entitlement REST endpoints.
 *
 * @since 13.0.0
 */
@GuiceModule
public class EntitlementRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(Key.get(Router.class, Names.named("XacmlRouter"))).toProvider(XacmlRouterProvider.class)
                .in(Singleton.class);

        // PolicyResource configuration
        bind(PrivilegeManager.class).to(PolicyPrivilegeManager.class);
        bind(PrivilegeManagerFactory.class).to(DefaultPrivilegeManagerFactory.class);
        bind(new TypeLiteral<ExceptionMappingHandler<EntitlementException, ResourceException>>() {})
                .to(EntitlementsExceptionMappingHandler.class);
        bind(PolicyParser.class).to(JsonPolicyParser.class);
        bind(PolicyStoreProvider.class).to(PrivilegePolicyStoreProvider.class);
        bind(new TypeLiteral<Map<Integer, Integer>>() {})
                .annotatedWith(Names.named(EntitlementsExceptionMappingHandler.RESOURCE_ERROR_MAPPING))
                .toProvider(EntitlementsResourceErrorMappingProvider.class)
                .asEagerSingleton();

        // Error code overrides for particular request types. Maps NOT FOUND errors on Create requests to BAD REQUESTs.
        final Map<RequestType, Map<Integer, Integer>> errorCodeOverrides =
                new EnumMap<RequestType, Map<Integer, Integer>>(RequestType.class);
        errorCodeOverrides.put(RequestType.CREATE,
                Collections.singletonMap(ResourceException.NOT_FOUND, ResourceException.BAD_REQUEST));

        bind(new TypeLiteral<Map<RequestType, Map<Integer, Integer>>>() {})
                .annotatedWith(Names.named(EntitlementsExceptionMappingHandler.REQUEST_TYPE_ERROR_OVERRIDES))
                .toInstance(errorCodeOverrides);

        bind(new TypeLiteral<Map<Integer, Integer>>() {})
                .annotatedWith(Names.named(EntitlementsExceptionMappingHandler.DEBUG_TYPE_OVERRIDES))
                .toProvider(EntitlementsResourceDebugMappingProvider.class)
                .asEagerSingleton();

        bind(new TypeLiteral<Map<String, QueryAttribute>>() {})
                .annotatedWith(Names.named(PrivilegePolicyStoreProvider.POLICY_QUERY_ATTRIBUTES))
                .toProvider(PolicyQueryAttributesMapProvider.class)
                .asEagerSingleton();
        bind(PolicyEvaluatorFactory.class).to(EntitlementEvaluatorFactory.class).in(Singleton.class);

        bind(new TypeLiteral<Map<String, QueryAttribute>>() {})
                .annotatedWith(Names.named(ApplicationsResource.APPLICATION_QUERY_ATTRIBUTES))
                .toProvider(ApplicationQueryAttributesMapProvider.class)
                .asEagerSingleton();

        bind(CollectionResourceProvider.class).annotatedWith(Names.named("PolicyResource")).to(PolicyResource.class);
    }

    /**
     * Provides the mapping between entitlements exceptions and CREST resource exceptions, based on the entitlements
     * error code. Anything not explicitly mapped here will be treated as an internal server error.
     */
    private static class EntitlementsResourceErrorMappingProvider implements Provider<Map<Integer, Integer>> {
        @Override
        public Map<Integer, Integer> get() {
            final Map<Integer, Integer> handlers = new HashMap<Integer, Integer>();

            handlers.put(EntitlementException.EMPTY_PRIVILEGE_NAME, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.NULL_ENTITLEMENT, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.UNSUPPORTED_OPERATION, ResourceException.NOT_SUPPORTED);
            handlers.put(EntitlementException.INVALID_XML, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_WSDL_LOCATION, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.MISSING_PRIVILEGE_JSON, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.SESSION_HAS_EXPIRED, ResourceException.FORBIDDEN);
            handlers.put(EntitlementException.INVALID_JSON, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.MISSING_PRIVILEGE_NAME, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.NO_SUCH_POLICY, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.APPLICATION_ALREADY_EXISTS, ResourceException.CONFLICT);
            handlers.put(EntitlementException.NO_SUCH_REFERRAL_PRIVILEGE, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.INCONSISTENT_WILDCARDS, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_PORT, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.MALFORMED_URL, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_RESOURCE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.NO_SUCH_APPLICATION, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.NOT_FOUND, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.PERMISSION_DENIED, ResourceException.FORBIDDEN);
            handlers.put(EntitlementException.SUBJECT_REQUIRED, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_SEARCH_FILTER, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_PROPERTY_VALUE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.START_DATE_AFTER_END_DATE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.MISSING_RESOURCE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.JSON_PARSE_ERROR, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.AUTHENTICATION_ERROR, ResourceException.FORBIDDEN);
            handlers.put(EntitlementException.INVALID_VALUE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_NAME_MISMATCH, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.APP_RETRIEVAL_ERROR, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.UNKNOWN_POLICY_CLASS,         ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_CLASS_CAST_EXCEPTION,  ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_CLASS_NOT_INSTANTIABLE,ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_CLASS_NOT_ACCESSIBLE,  ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_PROPERTY_VALUE_UNKNOWN_VALUE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_ALREADY_EXISTS, ResourceException.CONFLICT);
            handlers.put(EntitlementException.END_IP_BEFORE_START_IP, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.IP_CONDITION_CONFIGURATION_REQUIRED, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.PAIR_PROPERTY_NOT_DEFINED, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.APP_NOT_CREATED_POLICIES_EXIST, ResourceException.CONFLICT);
            handlers.put(EntitlementException.INVALID_APP_TYPE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_APP_REALM, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.PROPERTY_CONTAINS_BLANK_VALUE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.APPLICATION_NAME_MISMATCH, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.INVALID_CLASS, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.RESOURCE_TYPE_ALREADY_EXISTS, ResourceException.CONFLICT);
            handlers.put(EntitlementException.NO_SUCH_RESOURCE_TYPE, ResourceException.NOT_FOUND);
            handlers.put(EntitlementException.RESOURCE_TYPE_IN_USE, ResourceException.CONFLICT);
            handlers.put(EntitlementException.MISSING_RESOURCE_TYPE_NAME, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.RESOURCE_TYPE_REFERENCED, ResourceException.CONFLICT);
            handlers.put(EntitlementException.INVALID_RESOURCE_TYPE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.POLICY_DEFINES_INVALID_RESOURCE_TYPE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.MISSING_RESOURCE_TYPE, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.CONDITION_EVALUATION_FAILED, ResourceException.INTERNAL_ERROR);
            handlers.put(EntitlementException.RESOURCE_TYPE_ID_MISMATCH, ResourceException.BAD_REQUEST);
            handlers.put(EntitlementException.NO_RESOURCE_TYPE_EXPECTED, ResourceException.BAD_REQUEST);

            return handlers;
        }
    }

    public static Map<Integer, Integer> getEntitlementsErrorHandlers() {
        return new EntitlementsResourceErrorMappingProvider().get();
    }

    private static class EntitlementsResourceDebugMappingProvider implements Provider<Map<Integer, Integer>> {
        @Override
        public Map<Integer, Integer> get() {
            final Map<Integer, Integer> handlers = new HashMap<Integer, Integer>();
            handlers.put(EntitlementException.NO_SUCH_POLICY, Debug.MESSAGE);

            return handlers;
        }
    }

    /**
     * Defines all allowed query attributes in queries against the policy endpoint.
     */
    private static class PolicyQueryAttributesMapProvider implements Provider<Map<String, QueryAttribute>> {
        @Override
        public Map<String, QueryAttribute> get() {
            final Map<String, QueryAttribute> attributes = new HashMap<String, QueryAttribute>();

            attributes.put("name", new QueryAttribute(STRING, Privilege.NAME_SEARCH_ATTRIBUTE));
            attributes.put("description", new QueryAttribute(STRING, Privilege.DESCRIPTION_SEARCH_ATTRIBUTE));
            attributes.put("applicationName", new QueryAttribute(STRING, Privilege.APPLICATION_SEARCH_ATTRIBUTE));
            attributes.put("createdBy", new QueryAttribute(STRING, Privilege.CREATED_BY_SEARCH_ATTRIBUTE));
            attributes.put("creationDate", new QueryAttribute(TIMESTAMP, Privilege.CREATION_DATE_SEARCH_ATTRIBUTE));
            attributes.put("lastModifiedBy", new QueryAttribute(STRING, Privilege.LAST_MODIFIED_BY_SEARCH_ATTRIBUTE));
            attributes.put("lastModifiedDate", new QueryAttribute(TIMESTAMP, Privilege.LAST_MODIFIED_DATE_SEARCH_ATTRIBUTE));
            attributes.put("resourceTypeUuid", new QueryAttribute(STRING, Privilege.RESOURCE_TYPE_UUID_SEARCH_ATTRIBUTE));

            return attributes;
        }
    }

    /**
     * Defines all allowed query attributes in queries against the application endpoint.
     */
    private static class ApplicationQueryAttributesMapProvider implements Provider<Map<String, QueryAttribute>> {
        @Override
        public Map<String, QueryAttribute> get() {
            final Map<String, QueryAttribute> attributes = new HashMap<String, QueryAttribute>();

            attributes.put("name", new QueryAttribute(STRING, Application.NAME_SEARCH_ATTRIBUTE));
            attributes.put("description", new QueryAttribute(STRING, Application.DESCRIPTION_SEARCH_ATTRIBUTE));
            attributes.put("createdBy", new QueryAttribute(STRING, Application.CREATED_BY_SEARCH_ATTRIBUTE));
            attributes.put("creationDate", new QueryAttribute(TIMESTAMP, Application.CREATION_DATE_SEARCH_ATTRIBUTE));
            attributes.put("lastModifiedBy", new QueryAttribute(STRING, Application.LAST_MODIFIED_BY_SEARCH_ATTRIBUTE));
            attributes.put("lastModifiedDate", new QueryAttribute(TIMESTAMP, Application.LAST_MODIFIED_DATE_SEARCH_ATTRIBUTE));

            return attributes;
        }
    }
}
