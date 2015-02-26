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

import javax.servlet.http.HttpServletRequest;

import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.security.Enroler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * An OpenAMServletAuthenticator gets the {@link SSOToken} from the
 * {@link HttpServletRequest} and authenticates the
 * {@link org.restlet.security.User}.
 * <p/>
 * This class works with the Servlet Extension only!!!
 *
 */
public class OpenAMServletAuthenticator extends AbstractOpenAMAuthenticator {

    public OpenAMServletAuthenticator(Context context, OpenAMParameters parameters) {
        super(context, parameters);
    }

    public OpenAMServletAuthenticator(Context context, OpenAMParameters parameters, boolean optional) {
        super(context, parameters, optional);
    }

    public OpenAMServletAuthenticator(Context context, OpenAMParameters parameters,
            boolean multiAuthenticating, boolean optional, Enroler enroler) {
        super(context, parameters, multiAuthenticating, optional, enroler);
    }

    public OpenAMServletAuthenticator(Context context, OpenAMParameters parameters,
            boolean optional, Enroler enroler) {
        super(context, parameters, optional, enroler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SSOToken getToken(Request request, Response response) throws SSOException {
        SSOToken token = null;
        HttpServletRequest servletRequest = ServletUtils.getRequest(request);
        if (null != servletRequest) {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            token = manager.createSSOToken(servletRequest);
        }
        return token;
    }
}
