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
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link ResourceErrorHandler} for entitlements exceptions that translates errors based on the
 * {@link com.sun.identity.entitlement.EntitlementException#getErrorCode()}.
 *
 * @since 12.0.0
 */
public final class EntitlementsResourceErrorHandler implements ResourceErrorHandler<EntitlementException> {
    public static final String RESOURCE_ERROR_MAPPING = "EntitlementsResourceErrorMapping";
    private final ConcurrentMap<Integer, Integer> errorCodeMapping;

    @Inject
    public EntitlementsResourceErrorHandler(@Named(RESOURCE_ERROR_MAPPING) Map<Integer, Integer> errorCodeMapping) {
        Reject.ifNull(errorCodeMapping);
        this.errorCodeMapping = new ConcurrentHashMap<Integer, Integer>(errorCodeMapping);
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
        Integer resourceErrorType = errorCodeMapping.get(error.getErrorCode());
        if (resourceErrorType == null) {
            resourceErrorType = ResourceException.INTERNAL_ERROR;
        }

        return ResourceException.getException(resourceErrorType, error.getMessage(), error);
    }
}
