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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.router;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import org.forgerock.guice.core.InjectorHolder;

/**
 * A proxy implementation of the DelegationEvaluator, which delegates all its calls to the "real" implementation.
 *
 * @since 12.0.0
 */
@Singleton
public class DelegationEvaluatorProxy implements DelegationEvaluator {

    /**
     * Enum to lazy init the DelegationEvaluator variable in a thread safe manner.
     */
    private enum DelegationEvaluatorHolder {
        INSTANCE;

        private final DelegationEvaluator delegationEvaluator;

        private DelegationEvaluatorHolder() {
            delegationEvaluator = InjectorHolder.getInstance(DelegationEvaluatorImpl.class);
        }

        static DelegationEvaluator get() {
            return INSTANCE.delegationEvaluator;
        }
    }

    @Override
    public boolean isAllowed(SSOToken token, DelegationPermission permission, Map<String, Set<String>> envParameters)
            throws SSOException, DelegationException {
        return DelegationEvaluatorHolder.get().isAllowed(token, permission, envParameters);
    }

    @Override
    public boolean isAllowed(SSOToken token, DelegationPermission permission, Map<String, Set<String>> envParameters,
            boolean subTreeMode) throws SSOException, DelegationException {
        return DelegationEvaluatorHolder.get().isAllowed(token, permission, envParameters, subTreeMode);
    }
}
