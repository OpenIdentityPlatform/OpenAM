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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * A very simple class, intended to only represent the token_type of X509 in the token transformation invocation to the
 * rest-sts.
 */
public class X509TokenState {


    public static X509TokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        return new X509TokenState();
    }

    public JsonValue toJson() throws IllegalStateException {
        return json(object(field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.X509.name())));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof X509TokenState;
    }

    @Override
    public int hashCode() {
        return TokenType.X509.name().hashCode();
    }
}
