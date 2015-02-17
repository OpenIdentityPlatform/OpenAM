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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOToken;

/**
 * Interface defining utility functionality to identify if a SSOToken corresponds to an authenticated Soap STS Agent.
 */
public interface SoapSTSAgentIdentity {
    /**
     *
     * @param token The SSOToken instance corresponding to an authenticated caller.
     * @return true if the SSOToken corresponds to an authenticated soap-sts agent. False is returned if this is not the
     * case, or if an exception is thrown in the process of making the determination.
     */
    boolean isSoapSTSAgent(SSOToken token);
}
