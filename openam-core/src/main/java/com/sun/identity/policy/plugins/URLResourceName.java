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
 * $Id: URLResourceName.java,v 1.5 2009/05/07 19:02:15 dillidorai Exp $
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.interfaces.ResourceName;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.shared.resourcename.BaseURLResourceName;

import static com.sun.identity.policy.ResourceMatch.*;

/**
 * This plugin extends the functionality provided in
 * <code>PrefixResourceName</code> to provide special handling to
 * URL type prefix resource names in <code>canonicalize</code> method
 * like validating port, assigning default port of 80, if port absent etc.
 */
public class URLResourceName extends BaseURLResourceName<ResourceMatch, PolicyException> implements ResourceName {

    /**
     * empty no argument constructor.
     */
    public URLResourceName() {
        super(Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME), EXACT_MATCH, NO_MATCH, SUB_RESOURCE_MATCH,
                SUPER_RESOURCE_MATCH, WILDCARD_MATCH);
    }

    @Override
    protected PolicyException constructResourceInvalidException(Object[] args) {
        return new PolicyException(ResBundleUtils.rbName, "invalid_port_number", args, null);
    }

}
