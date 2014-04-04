/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

/**
 * Handles the authentication of the OAuth2 Resource Owner for the Password Credentials Grant Type.
 * <br/>
 * Interface to be extended and/or implemented for the Password Credentials Grant Type to provide the required
 * authentication of the resource owner.
 *
 * @since 12.0.0
 */
public interface ResourceOwnerPasswordAuthenticationHandler extends AuthenticationHandler {

    /**
     * Gets the resource owner's username.
     *
     * @return The specified username.
     */
    String getUsername();

    /**
     * Gets the resource owner's password.
     *
     * @return The specified password.
     */
    char[] getPassword();
}
