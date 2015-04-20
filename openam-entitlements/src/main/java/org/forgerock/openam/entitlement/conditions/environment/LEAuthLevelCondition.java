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
 * Copyright 2006 Sun Microsystems Inc.
 */
/*
 * Portions Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;

import java.util.Map;
import java.util.Set;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * requested auth level is less than or equal to the auth level set in the condition.
 *
 * @since 12.0.0
 */
public class LEAuthLevelCondition extends AuthLevelCondition {

    /**
     * Constructs a new LEAuthLevelCondition instance.
     */
    public LEAuthLevelCondition() {
        super();
    }

    /**
     * Constructs a new LEAuthLevelCondition instance.
     *
     * @param debug A Debug instance.
     * @param authUtils An instance of the AMAuthUtilsWrapper.
     */
    LEAuthLevelCondition(Debug debug, CoreWrapper authUtils) {
        super(debug, authUtils);
    }

    /**
     * Returns {@code true} if the requested auth level is less than or equal to the configured auth level.
     *
     * @param maxRequestAuthLevel {@inheritDoc}
     * @param advices {@inheritDoc}
     * @return {@code true} if the get {@code maxRequestAuthLevel} is less than or equal to the configured auth level.
     */
    @Override
    protected boolean isAllowed(int maxRequestAuthLevel, Map<String, Set<String>> advices) {
        return maxRequestAuthLevel <= getAuthLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConditionName() {
        return "LEAuthLevelCondition";
    }
}
