/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam.server;

import org.forgerock.restlet.ext.openam.OpenAMAuthenticatorHelper;
import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Enroler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * An OpenAMAuthenticator gets the {@link SSOToken} from the
 * {@code Authorization: OpenAM } header value and authenticate the
 * {@link org.restlet.security.User}.
 * <p/>
 * This class depend on OpenAM SDK only and support non-servlet deployment.
 *
 */
public class OpenAMAuthenticator extends AbstractOpenAMAuthenticator {

    /**
     * {@inheritDoc}
     */
    public OpenAMAuthenticator(Context context, OpenAMParameters parameters) {
        super(context, parameters);
    }

    /**
     * {@inheritDoc}
     */
    public OpenAMAuthenticator(Context context, OpenAMParameters parameters, boolean optional) {
        super(context, parameters, optional);
    }

    /**
     * {@inheritDoc}
     */
    public OpenAMAuthenticator(Context context, OpenAMParameters parameters,
            boolean multiAuthenticating, boolean optional, Enroler enroler) {
        super(context, parameters, multiAuthenticating, optional, enroler);
    }

    /**
     * {@inheritDoc}
     */
    public OpenAMAuthenticator(Context context, OpenAMParameters parameters, boolean optional,
            Enroler enroler) {
        super(context, parameters, optional, enroler);
    }

    @Override
    protected SSOToken getToken(Request request, Response response) throws SSOException {
        SSOToken token = null;
        if (request.getChallengeResponse() != null) {
            String tokenId =
                    OpenAMAuthenticatorHelper.retrieveSSOToken(request.getChallengeResponse());
            if (tokenId != null) {
                SSOTokenManager manager = SSOTokenManager.getInstance();
                token = manager.createSSOToken(tokenId);
            }
        }
        return token;
    }
}
