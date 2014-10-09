/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.rest.resource;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.json.resource.Context;

import javax.security.auth.Subject;

/**
 * CREST context interface for ServerContexts that allow retrieving an authenticated caller subject.
 *
 * @since 12.0.0
 */
public interface SubjectContext extends Context {

    /**
     * Returns the authenticated subject associated with this request.
     *
     * @return the authenticated subject associated with this request, or null if not authenticated.
     */
    Subject getCallerSubject();

    /**
     * Given a token Id returns the subject representation.
     * If the token Id is null or invalid, a null subject will be returned.
     *
     * @param tokenId
     *         a valid token Id
     *
     * @return the corresponding subject, or null if the token Id is invalid
     */
    Subject getSubject(String tokenId);

    /**
     * Returns the authenticated subjects sso token associated with this request.
     *
     * @return the SSO token associated with this request
     *
     * @throws SSOException
     *         should an error occur retrieving the token
     */
    SSOToken getCallerSSOToken() throws SSOException;

}
