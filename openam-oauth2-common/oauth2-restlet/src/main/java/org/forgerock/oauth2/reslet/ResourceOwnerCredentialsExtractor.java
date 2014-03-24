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

package org.forgerock.oauth2.reslet;

import org.forgerock.oauth2.core.InvalidClientException;
import org.forgerock.oauth2.core.InvalidRequestException;
import org.forgerock.oauth2.core.ResourceOwnerAuthentication;
import org.restlet.Request;

import static org.forgerock.oauth2.reslet.RestletUtils.getAttribute;

/**
 * @since 12.0.0
 */
public abstract class ResourceOwnerCredentialsExtractor {

    public ResourceOwnerAuthentication extract(final Request request) throws InvalidClientException, InvalidRequestException {

        final String username = getAttribute(request, "username");
        final char[] password = getAttribute(request, "password") == null ? null :
                getAttribute(request, "password").toCharArray();

        return createResourceOwnerAuthentication(request, username, password);
    }

    protected abstract ResourceOwnerAuthentication createResourceOwnerAuthentication(final Request request,
            final String username, final char[] password);
}
