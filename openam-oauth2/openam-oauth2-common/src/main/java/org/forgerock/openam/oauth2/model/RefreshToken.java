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

/**
 * Implements the OAuth2 Refresh Token
 * @see <a href="http://tools.ietf.org/html/rfc6749#section-1.5">1.5.  Refresh Token</a>
 */
public interface RefreshToken extends Token {

    /**
     * Get parent token
     * 
     * @return ID of parent token, will be null or an Access Code used to create this token
     */
    public String getParentToken();
}
