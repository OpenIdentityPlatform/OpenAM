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
package org.forgerock.openam.entitlement.rest;

import static org.forgerock.openam.entitlement.rest.ApplicationV1Filter.*;
import static org.forgerock.util.promise.Promises.*;

import com.sun.identity.entitlement.EntitlementException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;

/**
 * Transformer for the ApplicationV1Filter, to ensure that we get the responses in the correct v1 format.
 */
public class ApplicationV1FilterTransformer {

    private final ContextHelper contextHelper;
    private final ResourceTypeService resourceTypeService;
    private final ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler;

    @Inject
    public ApplicationV1FilterTransformer(ContextHelper contextHelper, ResourceTypeService resourceTypeService,
            ExceptionMappingHandler<EntitlementException, ResourceException> resourceErrorHandler) {
        this.contextHelper = contextHelper;
        this.resourceTypeService = resourceTypeService;
        this.resourceErrorHandler = resourceErrorHandler;
    }

    /**
     * Given the json representation of an application swaps out the resource type UUIDs for a set of actions and
     * resources that is the union of actions and resources represented by the associated resource types.
     *
     * @param jsonValue
     *         application json
     * @param callingSubject
     *         the calling subject
     * @param realm
     *         the realm
     *
     * @throws com.sun.identity.entitlement.EntitlementException
     *         should an error occur during transformation
     */
    public void transformJson(final JsonValue jsonValue, final Subject callingSubject, final String realm)
            throws EntitlementException {

        final Map<String, Boolean> actions = new HashMap<>();
        final Set<String> resources = new HashSet<>();

        final Set<String> resourceTypeUuids = jsonValue
                .get(RESOURCE_TYPE_UUIDS)
                .required()
                .asSet(String.class);

        for (String resourceTypeUuid : resourceTypeUuids) {
            final ResourceType resourceType = resourceTypeService
                    .getResourceType(callingSubject, realm, resourceTypeUuid);

            if (resourceType == null) {
                throw new EntitlementException(EntitlementException.NO_SUCH_RESOURCE_TYPE, resourceTypeUuid);
            }

            actions.putAll(resourceType.getActions());
            resources.addAll(resourceType.getPatterns());
        }

        jsonValue.remove(RESOURCE_TYPE_UUIDS);
        jsonValue.remove(APPLICATION_DISPLAY_NAME);
        jsonValue.add(ACTIONS, actions);
        jsonValue.add(RESOURCES, resources);
        jsonValue.add(REALM, realm);
    }

    /**
     * Transforms a response to be v1 compliant.
     *
     * @param promise The current response.
     * @param context Context of the request.
     * @return a v1 compliant response-promise.
     */
    public Promise<ResourceResponse, ResourceException> transform(Promise<ResourceResponse, ResourceException> promise,
                                                                   final Context context) {
        return promise
                .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(ResourceResponse response) {
                        JsonValue jsonValue = response.getContent();
                        Subject callingSubject = contextHelper.getSubject(context);
                        String realm = contextHelper.getRealm(context);

                        try {
                            transformJson(jsonValue, callingSubject, realm);
                        } catch (EntitlementException eE) {
                            return resourceErrorHandler.handleError(eE).asPromise();
                        }

                        return newResultPromise(response);
                    }
                });
    }

    /**
     * Transforms a response to a query to be v1 compliant.
     *
     * @param promise The current response.
     * @param context Context of the request.
     * @param request The request.
     * @param handler Query resource handler.
     * @param resources The collection of resources to respond.
     * @return a v1 compliant response-promise.
     */
    public Promise<QueryResponse, ResourceException> transform(Promise<QueryResponse, ResourceException> promise,
                                                                final Context context, final QueryRequest request,
                                                                final QueryResourceHandler handler,
                                                                final Collection<ResourceResponse> resources) {
        return promise
                .thenAsync(new AsyncFunction<QueryResponse, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(QueryResponse response) {
                        Subject callingSubject = contextHelper.getSubject(context);
                        String realm = contextHelper.getRealm(context);
                        try {
                            for (ResourceResponse resource : resources) {
                                final JsonValue jsonValue = resource.getContent();
                                transformJson(jsonValue, callingSubject, realm);
                                handler.handleResource(resource);
                            }
                        } catch (EntitlementException eE) {
                            return resourceErrorHandler.handleError(context, request, eE).asPromise();
                        }

                        return newResultPromise(response);
                    }
                });
    }

}
