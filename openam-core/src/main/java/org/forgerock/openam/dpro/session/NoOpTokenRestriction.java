/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.dpro.session;

import com.iplanet.dpro.session.TokenRestriction;

/**
 * A simple TokenRestriction implementation which will always satisfy the restriction.
 * @author Peter Major
 */
public class NoOpTokenRestriction implements TokenRestriction {

    /**
     * Tells whether this given restriction is satisfied, this implementation will always return true.
     * @param context The context from which the restriction needs to be
     *        checked. The context can be any from the following - the Single
     *        Sign on token of the Application against which the restriction
     *        is being compared - the IP Address/Host Name of the Application
     *        against which the restriction is being compared.
     * @return <code>true</code>, as this restriction is always satisfied.
     * @throws Exception this implementation does not throw exceptions.
     */
    public boolean isSatisfied(Object context) throws Exception {
        return true;
    }
}
