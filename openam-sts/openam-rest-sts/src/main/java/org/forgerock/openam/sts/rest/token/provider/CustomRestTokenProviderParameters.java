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

package org.forgerock.openam.sts.rest.token.provider;

import org.forgerock.json.JsonValue;

import java.security.Principal;

/**
 * A RestTokenProviderParameters subclass which adds additional state in support of custom RestTokenProvider implementations.
 */
public interface CustomRestTokenProviderParameters extends RestTokenProviderParameters<JsonValue> {
    /**
     *
     * @return  Return the Principal corresponding to successful token validation
     */
    Principal getPrincipal();

    /**
     *
     * @return the possibly null JsonValue corresponding the to additionalState possibly returned from the RestTokenTransformValidatorResult.
     * This is to support custom RestTokenTransformValidator and RestTokenProvider implementations.
     */
    JsonValue getAdditionalState();

    /**
     * Added to support custom RestTokenProvider implementations. Allows the custom RestTokenProvider to obtain the OpenAM
     * session id corresponding to successful token validation.
     * @return the OpenAM session id corresponding to the successfully-validated input token.
     */
    String getAMSessionIdFromTokenValidation();

}
