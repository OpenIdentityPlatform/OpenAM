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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.oauth2.core.exceptions.InvalidConfirmationKeyException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JwkConfirmationKeyValidator}.
 *
 * @since 14.0.0
 */
public final class JwkConfirmationKeyValidatorTest {

    private JwkConfirmationKeyValidator validator;

    @BeforeMethod
    public void setUp() {
        validator = new JwkConfirmationKeyValidator();
    }

    @Test
    public void acceptsValidJwk() throws InvalidConfirmationKeyException {
        validator.validate(json(object(field("jwk", object()))));
    }

    @Test(expectedExceptions = InvalidConfirmationKeyException.class)
    public void throwsExceptionWhenInvalidJwk() throws InvalidConfirmationKeyException {
        validator.validate(json(object(field("jwk", array()))));
    }

}