/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HttpURLResourceName.java,v 1.4 2008/06/25 05:43:51 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.shared.debug.Debug;

/**
 * This class was no longer in sync with expected policy evaluation and moved away from the behaviour of
 * {@link URLResourceName} which duplicates the behaviour of {@link com.sun.identity.entitlement.URLResourceName}
 * that is used by the server when evaluation a policy request in self mode.
 * <p />
 * To help reduce the cost of upgrade this class has been modified to ensure it continues with eager normalisation of
 * resource URLs but otherwise delegates directly to its subclass {@link URLResourceName}.
 *
 * @deprecated {@link URLResourceName} should be used in favour of this class ensuring the passed resources are
 * normalised via {@link URLResourceName#canonicalize(String)}.
 */
@Deprecated
public class HttpURLResourceName extends URLResourceName {

    public ResourceMatch compare(String requestResource, String targetResource, boolean wildcardCompare) {
        try {
            // Eagerly normalise the resource and target URLs.
            requestResource = canonicalize(requestResource);
            targetResource = canonicalize(targetResource);

        } catch (PolicyException pE) {
            debug.error("HttpURLResourceName#compare: Unable to normalise resource and target URLs", pE);
        }

        return super.compare(requestResource, targetResource, wildcardCompare);
    }

}
