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
 * $Id: URLResourceName.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.shared.resourcename.BaseURLResourceName;

import static com.sun.identity.entitlement.ResourceMatch.*;

/**
 * This plugin extends the functionality provided in
 * <code>PrefixResourceName</code> to provide special handling to
 * URL type prefix resource names in <code>canonicalize</code> method
 * like validating port, assigning default port of 80, if port absent etc.
 */
public class URLResourceName extends BaseURLResourceName<ResourceMatch, EntitlementException> implements ResourceName {

    /**
     * empty no argument constructor.
     */
    public URLResourceName() {
        super(Debug.getInstance("Entitlement"), EXACT_MATCH, NO_MATCH, SUB_RESOURCE_MATCH,
                SUPER_RESOURCE_MATCH, WILDCARD_MATCH);
    }

    @Override
    protected EntitlementException constructResourceInvalidException(Object[] args) {
        return new EntitlementException(EntitlementException.INVALID_PORT, args, null);
    }

    @Override
    protected String normalizeRequestResource(String requestResource) {
        return PrefixResourceName.doRequestResourceNormalization(requestResource, delimiter, wildcard);
    }

    @Override
    protected String normalizeTargetResource(String targetResource) {
        return PrefixResourceName.doTargetResourceNormalization(targetResource);
    }
}
            
