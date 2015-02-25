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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.entitlement.EntitlementException;

import java.util.Set;

public class EntitlementCoreWrapper {

    /**
     * Returns the set of all authenticated realm qualified service names.
     *
     * @param token valid user {@code SSOToken}.
     * @return Set containing String values representing realm qualified service names.
     * @throws EntitlementException if {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    Set<String> getRealmQualifiedAuthenticatedServices(SSOToken token) throws EntitlementException {
        try {
            return AMAuthUtils.getRealmQualifiedAuthenticatedServices(token);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    /**
     * Returns the set of all authenticated Realm names.
     *
     * @param token Valid user {@code SSOToken}
     * @return Set containing String values representing Realm names.
     * @throws EntitlementException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    Set<String> getAuthenticatedRealms(SSOToken token) throws EntitlementException {
        try {
            return AMAuthUtils.getAuthenticatedRealms(token);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    /**
     * Returns the set of all authenticated realm qualified scheme names.
     *
     * @param token A valid user {@code SSOToken}.
     * @return A {@code Set} containing String values representing realm qualified scheme names.
     * @throws EntitlementException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRealmQualifiedAuthenticatedSchemes(SSOToken token) throws EntitlementException {
        try {
            return AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(token);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    /**
     * Returns the set of all authenticated Scheme names.
     *
     * @param token A A valid user {@code SSOToken}.
     * @return A {@code Set} containing String values representing Scheme names.
     * @throws EntitlementException If {@code token.getProperty()} fails.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAuthenticatedSchemes(SSOToken token) throws EntitlementException {
        try {
            return AMAuthUtils.getAuthenticatedSchemes(token);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }
}
