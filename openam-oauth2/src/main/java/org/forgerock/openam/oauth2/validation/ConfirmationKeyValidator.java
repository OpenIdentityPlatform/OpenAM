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
 * Validates the passed json representation of a proof of possession confirmation key.
 *
 * @since 14.0.0
 */
public interface ConfirmationKeyValidator {

    /**
     * Validate the passed confirmation key.
     *
     * @param confirmationKey
     *         the confirmation key
     *
     * @throws InvalidConfirmationKeyException
     *         if the confirmation key is not a valid proof of possession key
     */
    void validate(JsonValue confirmationKey) throws InvalidConfirmationKeyException;

}
