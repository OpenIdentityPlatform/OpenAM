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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.SSOTokenIdentity
 * Note that this implementation will simply wrap consumption of the SSOTokenManager and IdUtils static methods. This
 * consumption has been placed behind an interface/impl to facilitate unit testing.
 */
public class SSOTokenIdentityImpl implements SSOTokenIdentity {
    public String validateAndGetTokenPrincipal(SSOToken subjectToken) throws TokenCreationException {
        try {
            if (SSOTokenManager.getInstance().isValidToken(subjectToken)) {
                return IdUtils.getIdentity(subjectToken).getName();
            } else {
                throw new TokenCreationException(ResourceException.FORBIDDEN,
                        "SSOToken corresponding to subject identity is invalid.");
            }
        } catch (SSOException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught getting identity from subject SSOToken in SSOTokenIdentityImpl: " + e, e);
        } catch (IdRepoException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught getting identity from subject SSOToken in SSOTokenIdentityImpl: " + e, e);
        }

    }
}
