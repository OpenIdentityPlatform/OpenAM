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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator;

/**
 * Defines the parameters passed to RestIssuedTokenValidator#validateToken invocations. Simply provides access to the
 * to-be-validated token, which are tokens issued by the rest-sts - currently SAML2 and OIDC tokens.
 */
public interface RestIssuedTokenValidatorParameters<T> {
    /**
     *
     * @return the to-be-validated token.
     */
    T getInputToken();
}
