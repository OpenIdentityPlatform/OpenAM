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

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.json.resource.ResourceException;

/**
 * This interface encapsulates the concerns of obtaining a soap-sts access token, necessary to consume the sts-publish
 * service, and to consume the token generation service. Each soap-sts instance will correspond to a soap-sts agent
 * account, and authenticate with these credentials. These credentials will be provided during soap-sts installation.
 */
public interface SoapSTSAccessTokenProvider {
    /**
     *
     * @return the SSOToken identifier corresponding to the authN of this soap-sts deployment. Won't return null.
     * @throws org.forgerock.json.resource.ResourceException if authentication failed for any reason.
     */
    public String getAccessToken() throws ResourceException;

    /**
     * Invalidates the session obtained by calling getAccessToken. Called immediately after consumption of the sts-publish
     * or token generation service, so that sessions don't build up on OpenAM
     * @param accessToken the accessToken returned fom getAccessToken.
     */
    public void invalidateAccessToken(String accessToken);
}
