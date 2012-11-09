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
package org.forgerock.openam.oauth2.model;

import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This implements the Access Token used in OAuth2. The Access Token is described in see
 * <a href="http://tools.ietf.org/html/rfc6749#section-1.4">http://tools.ietf.org/html/rfc6749#section-1.4</a>
 */
public interface AccessToken extends Token {

    /**
     * Gets the parent of the access token. Either the {@link RefreshToken}, or {@link AuthorizationCode}.
     * 
     * @return the parent token
     */
    public String getParentToken();

    /**
     * Gets the {@link RefreshToken} associated with this AccessToken.
     * <p/>
     * 
     * @return null if there is no RefreshToken associated
     */
    @JsonSerialize
    public String getRefreshToken();

    /**
     * Converts the token to Map
     * 
     * @return new Map representation of this AccessToken
     */
    public Map<String, Object> convertToMap();
}
