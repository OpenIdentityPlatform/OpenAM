/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openam.extensions.crowd;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.extensions.crowd.util.OpenAMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.confluence.user.ConfluenceAuthenticator;

/**
 * Confluence authenticator that works with OpenAM and Confluence 4.x.
 * 
 * Note that this functionality is based on the older {@link OpenAMConfluenceAuthenticator}.
 * 
 * @author Dave van Eijck
 *
 */
@SuppressWarnings("serial")
public class Confluence4Authenticator extends ConfluenceAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Confluence4Authenticator.class);

    /**
     * {@inheritDoc}
     * 
     * In this specific case it uses the OpenAM SDK client functionality to obtain the username from the OpenAM session and associate it to the Confluence user with the same username.
     */
    @Override
    public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
        Principal user = null;
        try {
            String username = OpenAMUtil.obtainUsername(request);
            LOGGER.debug("Got username = {}", username);

            if (username != null) {
                if (request.getSession() != null && request.getSession().getAttribute(LOGGED_IN_KEY) != null) {
                    user = getUserAccessor().getUser(((Principal) request.getSession().getAttribute(LOGGED_IN_KEY)).getName());
                    LOGGER.debug("Session found; user {} already logged in.", user.getName());
                } else {
                    user = getUserAccessor().getUser(username);

                    LOGGER.debug("Logged in via SSO, with User {}" + user.getName());

                    request.getSession().setAttribute(LOGGED_IN_KEY, user);
                    request.getSession().setAttribute(LOGGED_OUT_KEY, null);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while getting user.", e);
        }

        return user;
    }
}

