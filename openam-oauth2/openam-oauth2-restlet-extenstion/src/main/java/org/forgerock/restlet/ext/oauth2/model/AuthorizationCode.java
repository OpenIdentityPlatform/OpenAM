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
package org.forgerock.restlet.ext.oauth2.model;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface AuthorizationCode extends Token {

    /**
     * Checks if this code was used to issue a token or not
     * <p/>
     * If an authorization code is used more than once, the authorization server
     * MUST deny the request and SHOULD revoke (when possible) all tokens
     * previously issued based on that authorization code. The authorization
     * code is bound to the client identifier and redirection URI.
     * 
     * @return false if the code was not used to issue a access_token otherwise
     *         true
     */
    public boolean isTokenIssued();

    // public lifetime A maximum authorization code lifetime of 10 minutes is
    // RECOMMENDED
}
