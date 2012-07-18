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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.internal;

import org.forgerock.restlet.ext.oauth2.consumer.OAuth2User;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

/**
 * ONLY FOR DEMO!!!
 */
public class DefaultScopeEnroler implements Enroler {
    /**
     * Attempts to update an authenticated client, with a
     * {@link org.restlet.security.User} properly defined, by adding the
     * {@link org.restlet.security.Role} that are assigned to this user. Note
     * that principals could also be added to the
     * {@link org.restlet.data.ClientInfo} if necessary. The addition could also
     * potentially be based on the presence of {@link java.security.Principal}.
     * 
     * @param clientInfo
     *            The clientInfo to update.
     */
    public void enrole(ClientInfo clientInfo) {
        if (null != clientInfo && clientInfo.getUser() instanceof OAuth2User) {
            clientInfo.getRoles().clear();
            for (String scope : ((OAuth2User) clientInfo.getUser()).getScope()) {
                clientInfo.getRoles().add(new Role(scope, ""));
            }
        }
    }
}
