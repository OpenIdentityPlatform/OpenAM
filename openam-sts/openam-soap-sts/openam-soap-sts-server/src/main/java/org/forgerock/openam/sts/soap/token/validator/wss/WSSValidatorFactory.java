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

package org.forgerock.openam.sts.soap.token.validator.wss;

import org.apache.ws.security.validate.Validator;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;

/**
 * Defines the functionality necessary to produce instances of the org.apache.ws.security.validate.Validator instances
 * necessary to perform validation of the SupportingTokens specified in the SecurityPolicy bindings protecting soap-sts
 * instances. This will produce Validators necessary to validate x509 and UNT assertions. It will not produce
 * the Validators used to validate OpenAM tokens, as this is done via custom interceptor providers registered with the
 * CXF bus (non-standard tokens must be supported in this fashion).
 */
public interface WSSValidatorFactory {
    /**
     * @param tokenType The type of token which the Validator will validate
     * @param validationInvocationContext The context in which the Validator will be invoked.
     * @param invalidateInterimOpenAMSession Whether the interim OpenAM session, produced as part of token validation,
     *                                       should be invalidated at the conclusion of the token invocation.
     * @return The org.apache.ws.security.validate.Validator instance which can validate the specified tokenType
     * @throws java.lang.IllegalArgumentException If the specified TokenType is not supported. Currently supported
     * TokenTypes are USERNAME and X509
     */
    Validator getValidator(TokenType tokenType, ValidationInvocationContext validationInvocationContext,
                                  boolean invalidateInterimOpenAMSession) throws IllegalArgumentException;
}
