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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * A ClientAuthenticationFailureFactory which includes implementations of methods suitable for handling the OpenAM
 * implementation of OAuth2.
 *
 * @since 13.0.0
 */
public class OpenAMClientAuthenticationFailureFactory extends ClientAuthenticationFailureFactory {

    private RealmNormaliser realmNormaliser;
    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    /**
     * Creates the factory, and sets the realm normaliser
     * @param realmNormaliser An object used for normalising realms extracted from requests
     */
    @Inject
    public OpenAMClientAuthenticationFailureFactory(RealmNormaliser realmNormaliser) {
        this.realmNormaliser = realmNormaliser;
    }

    @Override
    protected boolean hasAuthorizationHeader(OAuth2Request request) {
        return request.<Request>getRequest().getChallengeResponse() != null;
    }

    @Override
    protected String getRealm(OAuth2Request request) {
        String realm = request.<String>getParameter(OAuth2Constants.Custom.REALM);
        try {
            realm = realmNormaliser.normalise(request.<String>getParameter(OAuth2Constants.Custom.REALM));
        } catch(NotFoundException e) {
            // We are currently constructing an exception, so we do not want to throw an exception unrelated to the
            // original cause. As such, we compromise by using the non-normalised realm.
            logger.debug("OpenAMClientAuthenticationFailureFactory: failed to normalise realm", e);
        }
        return realm;
    }
}
