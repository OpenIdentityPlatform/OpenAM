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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.data.Parameter;
import org.restlet.util.Series;

/**
 * Defines an abstract access token
 * <p/>
 * 
 * <pre>
 *  {
 *      "access_token":"mF_9.B5f-4.1JqM",
 *      "expires_in":3600,
 *      "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
 *  }
 * </pre>
 *
 */
public abstract class AbstractAccessToken implements Serializable {

    private static final long serialVersionUID = -125994904834458501L;
    protected String access_token;
    protected String refresh_token = null;
    protected Number expires_in;

    //
    protected String client_id = null;
    protected String username = null;
    protected Set<String> scope = null;

    private final Long received = System.currentTimeMillis();
    private final boolean validated;

    public AbstractAccessToken(Series<Parameter> token) {
        validated = false;
        access_token = token.getFirstValue(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
        if (OAuth2Utils.isBlank(access_token)) {
            throw OAuthProblemException.OAuthError.INVALID_TOKEN.handle(null, "Invalid access token");
        }
        String o = token.getFirstValue(OAuth2Constants.Token.OAUTH_EXPIRES_IN);
        if (o instanceof String) {
            // Todo Catch the exception
            expires_in = Long.decode(o);
        } else {
            expires_in = 0l;
        }
        o = token.getFirstValue(OAuth2Constants.Token.OAUTH_REFRESH_TOKEN);
        if (o instanceof String) {
            refresh_token = o;
        }
    }

    public AbstractAccessToken(Map<String, Object> token) {
        validated = false;
        Object o = token.get(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
        if (o instanceof String) {
            access_token = (String) o;
        } else {
            throw OAuthProblemException.OAuthError.INVALID_TOKEN.handle(null, "Invalid access token");
        }
        o = token.get(OAuth2Constants.Token.OAUTH_EXPIRES_IN);
        if (o instanceof String) {
            // Todo Catch the exception
            expires_in = Long.decode((String) o);
        } else if (o instanceof Number) {
            expires_in = (Number) o;
        }
        o = token.get(OAuth2Constants.Token.OAUTH_REFRESH_TOKEN);
        if (o instanceof String) {
            refresh_token = (String) o;
        }
    }

    public AbstractAccessToken(AbstractAccessToken copyToken, Number expires_in, String client_id,
            String username, Set<String> scope) {
        validated = true;
        access_token = copyToken.getAccessToken();
        refresh_token = copyToken.getRefreshToken();
        this.expires_in = expires_in;
        this.client_id = client_id;
        this.username = username;
        this.scope = scope;
    }

    public abstract String getTokenType();

    public String getAccessToken() {
        return access_token;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public boolean hasRefreshToken() {
        return OAuth2Utils.isNotBlank(getRefreshToken());
    }

    public Number getExpiresIn() {
        if (null == expires_in) {
            return 0l;
        }
        return expires_in;
    }

    // Todo Use some validation level
    public boolean isValid() {
        return (System.currentTimeMillis() - received) < expires_in.longValue() * 1000l;
    }

    public String getUsername() {
        return username;
    }

    public String getClientId() {
        return client_id;
    }

    public Set<String> getScope() {
        return scope;
    }
}
