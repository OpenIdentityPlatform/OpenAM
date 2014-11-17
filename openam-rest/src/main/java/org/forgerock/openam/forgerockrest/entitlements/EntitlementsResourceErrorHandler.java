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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link ResourceErrorHandler} for entitlements exceptions that translates errors based on the
 * {@link com.sun.identity.entitlement.EntitlementException#getErrorCode()}.
 *
 * @since 12.0.0
 */
public final class EntitlementsResourceErrorHandler implements ResourceErrorHandler<EntitlementException> {
    public static final String RESOURCE_ERROR_MAPPING = "EntitlementsResourceErrorMapping";
    public static final String REQUEST_TYPE_ERROR_OVERRIDES = "EntitlementsResourceRequestTypeErrorOverrides";

    /**
     * Mapping from EntitlementException error codes to ResourceException error codes.
     */
    private final Map<Integer, Integer> errorCodeMapping;

    /**
     * Mapping from request types to a mapping from ResourceException error codes to alternative
     * ResourceException error codes. Used to e.g., translate nonsensical "NotFound" errors in a
     * Create request into a more appropriate BadRequest error. Note that both keys and values in
     * the inner map are ResourceException error codes.
     *<p/>
     * Use case is user tries to create a policy/privilege for a non-existing application. Lookup of the application
     * fails and gets translated into a NOT_FOUND/404. This doesn't make sense on a Create as a 404 always refers to
     * the resource in the request (i.e., the one you were trying to create) rather than any dependencies. So we
     * translate it into a BAD_REQUEST instead.
     */
    private final Map<RequestType, Map<Integer, Integer>> errorCodeOverrides;

    /**
     * Constructs the error resource handler with the given mapping from EntitlementException error codes to
     * ResourceException error codes, and the given map of overrides of ResourceException error codes for particular
     * request types.
     *
     * @param errorCodeMapping general mapping from EntitlementException error codes to ResourceException error codes.
     * @param errorCodeOverrides per-request-type overrides of ResourceException error codes. The inner map keys and
     *                           values are both ResourceException error codes.
     */
    @Inject
    public EntitlementsResourceErrorHandler(
            @Named(RESOURCE_ERROR_MAPPING) Map<Integer, Integer> errorCodeMapping,
            @Named(REQUEST_TYPE_ERROR_OVERRIDES) Map<RequestType, Map<Integer, Integer>> errorCodeOverrides) {
        Reject.ifNull(errorCodeMapping);
        Reject.ifNull(errorCodeOverrides);

        this.errorCodeMapping = new HashMap<Integer, Integer>(errorCodeMapping);
        this.errorCodeOverrides = new EnumMap<RequestType, Map<Integer, Integer>>(RequestType.class);
        this.errorCodeOverrides.putAll(errorCodeOverrides);
    }

    /**
     * Constructs the resource error handler with the given error code mapping and no request-type specific overrides.
     *
     * @param errorCodeMapping the mapping from EntitlementException error codes to ResourceException error codes.
     */
    public EntitlementsResourceErrorHandler(Map<Integer, Integer> errorCodeMapping) {
        this(errorCodeMapping, Collections.<RequestType, Map<Integer, Integer>>emptyMap());
    }

    /**
     * Constructs an appropriate {@link ResourceException} for the given request and entitlements exception. If no
     * specific mapping is present for the error code in the exception, then an internal server error is reported.
     *
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate resource error.
     */
    @Override
    public ResourceException handleError(Request request, EntitlementException error) {

        EntitlementException errorToHandle =  causeOf(error);

        Integer resourceErrorType = errorCodeMapping.get(errorToHandle.getErrorCode());
        if (resourceErrorType == null) {
            resourceErrorType = ResourceException.INTERNAL_ERROR;
        }

        // Apply any request type specific overrides. E.g., NOT FOUND codes make no sense in Create requests.
        if (request != null) {
            Map<Integer, Integer> overrides = errorCodeOverrides.get(request.getRequestType());
            if (overrides != null && overrides.containsKey(resourceErrorType)) {
                resourceErrorType = overrides.get(resourceErrorType);
            }
        }

        return ResourceException.getException(resourceErrorType, errorToHandle.getMessage(), errorToHandle);
    }

    /**
     * If the provided exception occurred when deserializing JSON, this method will attempt to extract a more useful
     * error response than the generic "JSON string is invalid". Otherwise, the provided exception will be returned.
     *
     * @param ex The EntitlementException that occurred when attempting to handle the request.
     * @return The appropriate ResourceException for the provided EntitlementException.
     */
    private EntitlementException causeOf(EntitlementException ex) {

        if (ex.getErrorCode() == EntitlementException.INVALID_JSON) {
            if (ex.getCause() instanceof JsonMappingException) {
                if (ex.getCause().getCause() instanceof EntitlementException) {
                    EntitlementException cause = (EntitlementException) ex.getCause().getCause();
                    if (errorCodeMapping.containsKey(cause.getErrorCode())) {
                        return cause;
                    }
                }
            }
        }

        return ex;
    }

}
