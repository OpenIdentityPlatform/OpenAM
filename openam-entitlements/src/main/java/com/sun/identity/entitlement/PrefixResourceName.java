/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PrefixResourceName.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.shared.resourcename.BasePrefixResourceName;

import static com.sun.identity.entitlement.ResourceMatch.*;

/**
 * This is a plugin impelmentation of the <code>ResourceName</code> interface
 * it provides methods to do resource comparisons and resource
 * handling based on prefix based string match going left to right.
 * This kind of pattern matching would be used by URL kind of resources.
 */

public class PrefixResourceName extends BasePrefixResourceName<ResourceMatch, EntitlementException> implements ResourceName {

    private final static String DUMMY_URI = "dummy.html";
    
    //parameter wildcard is no longer required in the entitlement framework
    private final static String PARAM_WILDCARD = "*?*";

    /**
     * empty no argument constructor.
     */
    public PrefixResourceName() {
        super(Debug.getInstance("Entitlement"), EXACT_MATCH, NO_MATCH, SUB_RESOURCE_MATCH, SUPER_RESOURCE_MATCH,
                WILDCARD_MATCH);
    }

    @Override
    protected String normalizeRequestResource(String requestResource) {
        return doRequestResourceNormalization(requestResource, delimiter, wildcard);
    }

    @Override
    protected String normalizeTargetResource(String targetResource) {
        return doTargetResourceNormalization(targetResource);
    }

    /**
     * Normalize the request resource specifically for entitlement. Treat a resource with a delimiter at the end as a
     * directory, and get rid of ?* for entitlement engine. Also used by URLResourceName.
     * @param originalRequestResource The request resource.
     * @param delimiter The instance's delimiter (typically '/')
     * @param wildcard The wildcard string (typically '*')
     * @return The normalized resource.
     */
    static String doRequestResourceNormalization(String originalRequestResource, String delimiter, String wildcard) {
        String requestResource = originalRequestResource;
        String leftPrecedence = SystemPropertiesManager.get(Constants.DELIMITER_PREF_LEFT, Boolean.FALSE.toString());

        // end delimiter means we treat this resource as a directory
        if (Boolean.parseBoolean(leftPrecedence)) {
            if (requestResource.endsWith(delimiter)) {
                requestResource = requestResource + DUMMY_URI;
            } else if (requestResource.endsWith(delimiter + wildcard)) {
                requestResource = requestResource.substring(0, requestResource.length() - 1) + DUMMY_URI;
            }
        }

        // get rid of ending '?*' if any from requestResource
        // new entitlement engine no longer evaluates parameter wildcard
        while (requestResource.endsWith(PARAM_WILDCARD)) {
            int len = requestResource.length();
            requestResource = requestResource.substring(0, len - 2);
        }
        return requestResource;
    }

    /**
     * Normalize the request resource specifically for entitlement. Get rid of ?* for entitlement engine.  Also used by
     * URLResourceName.
     * @param originalTargetResource The target resource.
     * @return The normalized resource.
     */
    static String doTargetResourceNormalization(String originalTargetResource) {
        String targetResource = originalTargetResource;
        // get rid of ending '?*' if any from targetResource
        // new entitlement engine no longer evaluates parameter wildcard
        while (targetResource.endsWith(PARAM_WILDCARD)) {
            int len = targetResource.length();
            targetResource = targetResource.substring(0, len - 2);
        }
        return targetResource;
    }

    @Override
    protected EntitlementException constructResourceInvalidException(Object[] args) {
        return new EntitlementException(300);
    }

}
