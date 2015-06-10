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

package org.forgerock.openam.sts.soap.token.provider.oidc;

import org.forgerock.openam.sts.TokenTypeId;

import java.util.Set;

/**
 * An interface which allows soap-sts publishers to generate the amr claim for issued OpenIdConnect tokens on the basis
 * of the validated input token. See the amr claim here: http://openid.net/specs/openid-connect-core-1_0.html#IDToken.
 */
public interface SoapOpenIdConnectTokenAuthnMethodsReferencesMapper {
    /**
     * Returns the Set of authentication methods references corresponding to the TokenType inputToken.
     * @param inputTokenType The TokenType validated as part of the token transformation
     * @param inputToken The representation of the input token type, either passed as a ReceivedToken to the e.g. renew
     *                   operation, or the yield of SecurityPolicy binding traversal, or the ActAs/OnBehalfOf token
     *                   element passed to the issue operation. This state can be used by custom implementations of this interface
     *                   to make more elaborate decisions regarding the returned set of authentication methods references.
     * @return A valid set of Authentication Methods References, as specified in the amr claim here:
     * http://openid.net/specs/openid-connect-core-1_0.html#IDToken
     */
    Set<String> getAuthnMethodsReferences(TokenTypeId inputTokenType, Object inputToken);

}
