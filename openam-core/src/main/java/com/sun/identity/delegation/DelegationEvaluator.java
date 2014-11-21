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

package com.sun.identity.delegation;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import java.util.Map;
import java.util.Set;

/**
 * Evaluates the permission requests.
 *
 * @since 12.0.0
 */
public interface DelegationEvaluator {

    /**
     * Returns a boolean value indicating if a user has the specified permission.
     *
     * @param token
     *         SSO token of the user evaluating permission
     * @param permission
     *         delegation permission to be evaluated
     * @param envParameters
     *         run-time environment parameters
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws SSOException
     *         if single-sign-on token invalid or expired
     * @throws DelegationException
     *         for any other abnormal condition
     */
    boolean isAllowed(SSOToken token, DelegationPermission permission,
                      Map<String, Set<String>> envParameters) throws SSOException, DelegationException;

    /**
     * Returns a boolean value indicating if a user has the specified permission.
     *
     * @param token
     *         SSO token of the user evaluating permission
     * @param permission
     *         delegation permission to be evaluated
     * @param envParameters
     *         run-time environment parameters
     * @param subTreeMode
     *         whether to run in subtree mode or not
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws SSOException
     *         if single-sign-on token invalid or expired
     * @throws DelegationException
     *         for any other abnormal condition
     */
    boolean isAllowed(SSOToken token, DelegationPermission permission,
                      Map<String, Set<String>> envParameters, boolean subTreeMode)
            throws SSOException, DelegationException;

}
