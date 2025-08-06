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
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
 * Portions Copyrighted 2019-2025 3A Systems, LLC. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator;

import org.forgerock.openam.sts.TokenValidationException;

import com.iplanet.sso.SSOTokenManager;

import jakarta.inject.Inject;
import java.security.Principal;

public class PrincipalFromSessionImpl implements PrincipalFromSession {

    @Inject
    public PrincipalFromSessionImpl() {
    }

    @Override
    public Principal getPrincipalFromSession(String sessionId) throws TokenValidationException {
    	try {
    		return SSOTokenManager.getInstance().createSSOToken(sessionId).getPrincipal();
    	}catch (Exception e) {
			throw new TokenValidationException(401,"Session invalid",e);
		}
    }

}
