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

package org.forgerock.openam.oauth2.model.impl;

import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;

/**
 * TODO Description.
 */
public class SessionClientImpl extends JsonValue implements SessionClient {

    // TODO clean up cases in interface
    private String clientId;
    private String redirectUri;

    /**
     * TODO Description.
     * 
     * @param clientId
     *            TODO Description
     * @param redirectUri
     *            TODO Description
     */
    public SessionClientImpl(String clientId, String redirectUri) {
        super(new HashMap<String, Object>());
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getRedirectUri() {
        return redirectUri;
    }

}
