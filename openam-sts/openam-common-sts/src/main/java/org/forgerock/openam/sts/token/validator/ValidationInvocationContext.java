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

package org.forgerock.openam.sts.token.validator;

/**
 * This enum is passed to all TokenValidator implementations. TokenValidator implementations are responsible for
 * caching the OpenAM session identifiers corresponding to the validated token, as well as information concerning
 * whether this OpenAM session should be invalidated once any output token is generated. In the soap-sts case, in
 * support of delegated token validation (ActAs/OnBehalfOf as defined in WS-Trust), an OpenAM session identifier will
 * result from the validation of this delegated token, which is potentially in addition to the validation which will
 * occur as the result of SecurityPolicy binding enforcement. This OpenAM session id must be cached in a different
 * location, so it may be referenced when a SenderVouches SAML2 assertion is generated. This enum allows the various
 * validation contexts to be communicated to TokenValidator implementations, so that they can cache an OpenAM session
 * ids in the correct location.
 */
public enum ValidationInvocationContext {
    /*
    Token validation taking place in the context of standard rest-sts token transformation
     */
    REST_TOKEN_TRANSFORMATION,
    /*
    Token validation taking place in the context of soap-sts SecurityPolicy binding enforcement
     */
    SOAP_SECURITY_POLICY,
    /*
    Token validation taking place in the context of a delegated token - ActAs/OnBehalfOf as defined in WS-Trust
     */
    SOAP_TOKEN_DELEGATION,
    /*
    Token validation taking place as part of the WS-Trust defined Validate operation
     */
    SOAP_TOKEN_VALIDATION
}
