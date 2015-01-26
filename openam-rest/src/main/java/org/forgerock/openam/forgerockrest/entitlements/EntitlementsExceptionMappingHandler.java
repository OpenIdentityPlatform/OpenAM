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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.util.Reject;

/**
 * Default {@link org.forgerock.openam.errors.ExceptionMappingHandler} for entitlements exceptions that translates errors based on the
 * {@link com.sun.identity.entitlement.EntitlementException#getErrorCode()}.
 *
 * @since 12.0.0
 */
public final class EntitlementsExceptionMappingHandler
        implements ExceptionMappingHandler<EntitlementException, ResourceException> {
    public static final String RESOURCE_ERROR_MAPPING = "EntitlementsResourceErrorMapping";
    public static final String REQUEST_TYPE_ERROR_OVERRIDES = "EntitlementsResourceRequestTypeErrorOverrides";
    public static final String DEBUG_TYPE_OVERRIDES = "EntitlementResourceErrorDebug";

    private static final Debug DEBUG = Debug.getInstance("amPolicy");

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
     * Maps requests that don't need debug information to be attached to them using the default debug format.
     */
    private final Map<Integer, Integer> errorDebugMap;

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
    public EntitlementsExceptionMappingHandler(
            @Named(RESOURCE_ERROR_MAPPING) Map<Integer, Integer> errorCodeMapping,
            @Named(REQUEST_TYPE_ERROR_OVERRIDES) Map<RequestType, Map<Integer, Integer>> errorCodeOverrides,
            @Named(DEBUG_TYPE_OVERRIDES) Map<Integer, Integer> errorDebugMap) {
        Reject.ifNull(errorCodeMapping);
        Reject.ifNull(errorCodeOverrides);
        Reject.ifNull(errorDebugMap);

        this.errorDebugMap = new HashMap<Integer, Integer>(errorDebugMap);
        this.errorCodeMapping = new HashMap<Integer, Integer>(errorCodeMapping);
        this.errorCodeOverrides = new EnumMap<RequestType, Map<Integer, Integer>>(RequestType.class);
        this.errorCodeOverrides.putAll(errorCodeOverrides);
    }

    /**
     * Constructs the resource error handler with the given error code mapping and no request-type specific overrides.
     *
     * @param errorCodeMapping the mapping from EntitlementException error codes to ResourceException error codes.
     */
    public EntitlementsExceptionMappingHandler(Map<Integer, Integer> errorCodeMapping) {
        this(errorCodeMapping, Collections.<RequestType, Map<Integer, Integer>>emptyMap(),
                Collections.<Integer, Integer>emptyMap());
    }

    /**
     * Constructs an appropriate {@link ResourceException} for the given request and entitlements exception. If no
     * specific mapping is present for the error code in the exception, then an internal server error is reported.
     *
     * @param context the server context from which the language header can be read.
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate resource error.
     */
    @Override
    public ResourceException handleError(Context context, Request request, EntitlementException error) {
        return handleError(context, null, request, error);
    }

    /**
     * Constructs an appropriate {@link ResourceException} for the given request and entitlements exception. If no
     * specific mapping is present for the error code in the exception, then an internal server error is reported. Will
     * also write a debug entry - defaulting at the ERROR level. This level can be changed by mapping the
     * appropriate resource error to the desired debug level.
     *
     * @param context the server context from which the language header can be read.
     * @param msg the debug message to write.
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate resource error.
     */
    @Override
    public ResourceException handleError(Context context, String msg, Request request, EntitlementException error) {

        EntitlementException errorToHandle = causeOf(error);

        Integer resourceErrorType = errorCodeMapping.get(errorToHandle.getErrorCode());
        if (resourceErrorType == null) {
            resourceErrorType = ResourceException.INTERNAL_ERROR;
        }

        if (!StringUtils.isBlank(msg)) {
            final Integer debugOverride = errorDebugMap.get(errorToHandle.getErrorCode());
            debug(debugOverride, msg, error);
        }

        // Apply any request type specific overrides. E.g., NOT FOUND codes make no sense in Create requests.
        if (request != null) {
            Map<Integer, Integer> overrides = errorCodeOverrides.get(request.getRequestType());
            if (overrides != null && overrides.containsKey(resourceErrorType)) {
                resourceErrorType = overrides.get(resourceErrorType);
            }
        }

        return ResourceException.getException(resourceErrorType, getLocalizedMessage(context, error), errorToHandle);
    }

    /**
     * Constructs an appropriate {@link ResourceException} for the given request and entitlements exception. If no
     * specific mapping is present for the error code in the exception, then an internal server error is reported. Will
     * also write a debug entry - defaulting at the ERROR level. This level can be changed by mapping the
     * appropriate resource error to the desired debug level.
     *
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @param msg the debug message to write.
     * @return an appropriate resource error.
     */
    @Override
    public ResourceException handleError(String msg, Request request, EntitlementException error) {
        return handleError(null, msg, request, error);
    }

    /**
     * Writes out the provided debug message to the indicates debug.
     * @param debugOverride of the types Debug.ERROR, Debug.WARNING or Debug.MESSAGE.
     * @param msg to write to the debug log.
     * @param error the error triggering this debug message.
     */
    private void debug(Integer debugOverride, String msg, EntitlementException error) {

        if (debugOverride == null) {
            debugOverride = Debug.ERROR;
        }

        switch (debugOverride) {
            case Debug.WARNING:
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning(msg, error);
                }
                break;
            case Debug.MESSAGE:
                if (DEBUG.messageEnabled()) {
                    DEBUG.message(msg); //no error in msg
                }
                break;
            default:
                if (DEBUG.errorEnabled()) {
                    DEBUG.error(msg, error);
                }
                break;
        }
    }

    @Override
    public ResourceException handleError(Request request, EntitlementException error) {
        return handleError(null, null, request, error);
    }

    @Override
    public ResourceException handleError(EntitlementException error) {
        return handleError(null, error);
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

    /**
     * Get the localized message for the specified entitlement exception.
     * @param context The server context from which the language header can be read.
     * @param exception The exception that contains the message.
     * @return The localized message.
     */
    private String getLocalizedMessage(Context context, EntitlementException exception) {
        final Locale local = ServerContextUtils.getLocaleFromContext(context);

        if (local == null) {
            return exception.getMessage();
        } else {
            return exception.getLocalizedMessage(local);
        }
    }
}
