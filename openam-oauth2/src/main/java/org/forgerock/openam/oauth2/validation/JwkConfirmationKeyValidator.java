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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.oauth2.validation;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidConfirmationKeyException;

/**
 * Provides basic validation to verify the presence of a JWK_KEY.
 *
 * @since 14.0.0
 */
public final class JwkConfirmationKeyValidator implements ConfirmationKeyValidator {

    private static final String JWK_KEY = "jwk";

    @Override
    public void validate(JsonValue confirmationKey) throws InvalidConfirmationKeyException {
        if (!confirmationKey.isDefined(JWK_KEY) || !confirmationKey.get(JWK_KEY).isMap()) {
            throw new InvalidConfirmationKeyException("Invalid jwk as specified by the proof of possession spec");
        }
    }

}
