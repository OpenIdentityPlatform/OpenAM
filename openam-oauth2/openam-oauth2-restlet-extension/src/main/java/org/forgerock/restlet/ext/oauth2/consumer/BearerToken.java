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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.restlet.data.Parameter;
import org.restlet.util.Series;

/**
 * Implements a bearer token
 * <p/>
 * 
 * <pre>
 *  {
 *      "access_token":"mF_9.B5f-4.1JqM",
 *      "token_type":"Bearer",
 *      "expires_in":3600,
 *      "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
 *  }
 * </pre>
 *
 */
public class BearerToken extends AbstractAccessToken {

    private static final long serialVersionUID = 1894644624976193651L;

    public BearerToken(Series<Parameter> token) {
        super(token);
    }

    public BearerToken(Map<String, Object> token) {
        super(token);
    }

    public BearerToken(AbstractAccessToken copyToken, Number expires_in, String client_id,
            String username, Set<String> scope) {
        super(copyToken, expires_in, client_id, username, scope);
    }

    @Override
    public String getTokenType() {
        return OAuth2Constants.Bearer.BEARER;
    }
}
