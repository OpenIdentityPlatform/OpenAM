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

package org.forgerock.openam.noauth2.wrappers.restlet;

import org.forgerock.oauth2.core.ClientAuthentication;
import org.forgerock.oauth2.reslet.ClientCredentialsExtractor;
import org.forgerock.openam.noauth2.wrappers.OpenAMClientAuthentication;
import org.restlet.Request;

import static org.forgerock.oauth2.reslet.RestletUtils.*;

/**
 * @since 12.0.0
 */
public class OpenAMClientCredentialsExtractor extends ClientCredentialsExtractor {

    @Override
    protected ClientAuthentication createClientAuthentication(Request request, String clientId, String clientSecret) {
        String realm = getAttribute(request, "realm");
        if (realm == null) {
            realm = "/";
        }
        return new OpenAMClientAuthentication(clientId, clientSecret, realm);
    }
}
