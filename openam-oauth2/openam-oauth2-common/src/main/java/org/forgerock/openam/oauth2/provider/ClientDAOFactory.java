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

package org.forgerock.openam.oauth2.provider;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.oauth2.provider.impl.OpenAMClientDAO;
import org.restlet.Request;

/**
 * Creates a Client DAO
 */
public class ClientDAOFactory {

    /**
     * Creates a OpenAMClientDAO
     * @param realm The realm the ClientDAO is under.
     * @param request The HttpServletRequest to use.
     * @param token The SSOToken to use.
     * @return An OpenAMClientDAO.
     */
    public static OpenAMClientDAO newOpenAMClientDAO(String realm, Request request, SSOToken token) {
        return new OpenAMClientDAO(realm, request, token);
    }
}
