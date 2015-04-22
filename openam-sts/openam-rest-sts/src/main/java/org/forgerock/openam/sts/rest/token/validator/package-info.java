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

/**
 * Token validation occurs in three contexts in the sts:
 * 1. soap-sts SecurityPolicy binding enforcement: here a org.apache.ws.security.validate.Validator implementation must
 * be plugged-into the cxf wss4j runtime to validate tokens of a particular type
 * 2. soap-sts token validation: org.apache.cxf.sts.token.validator.TokenValidator implementations are responsible for
 * validating tokens as part of the WS-Trust defined Validate operation, and as part of delegated tokens (ActAs/OnBehalfOf)
 * tokens encapsulated in the RequestSecurityToken request targeting the WS-Trust defined issue operation. Note that the
 * org.apache.cxf.sts.token.validator.TokenValidator implementations will commonly delegate actual token validation to
 * wss4j org.apache.ws.security.validate.Validator implementations.
 * 3. rest-sts token validation: org.forgerock.openam.sts.rest.RestTokenValidator implementations which validate the
 * input token specified in a token transformation (current implementation), and the input token specified in a validate
 * operation (future feature).
 *
 * In all three cases, the {@code org.forgerock.openam.sts.token.validator.AuthenticationHandler<T>}, where T is a
 * particular token type, are plugged into all three
 * contexts to actual perform the token validation against the OpenAM rest authN context. Each sts instance is published
 * with AuthTargetMapping instances, which specify the rest authN target for each supported token type. The act of consuming
 * the OpenAM rest authN context boils down to 1. obtaining the appropriate rest authN url, using the AuthTargetMapping
 * state for the sts instance, functionality defined in the org.forgerock.openam.sts.token.validator.url package and
 * 2. actually POSTing the token state against the rest authN url, functionality defined by the
 * {@code org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher<T>} interface. Implementations
 * of this interface know how to post specific token state against the rest authN url.
 *
 * Thus, in all three cases, the {@code org.forgerock.openam.sts.token.validator.AuthenticationHandler<T>,
 * org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher<T> } are bound for the set of supported
 * token types, and plugged-in as the ultimate foundation of token validation in all three contexts.
 *
 * The {@code org.forgerock.openam.sts.token.validator.AuthenticationHandler<T>} interface specifies that the OpenAM
 * session id corresponding to the successfully-authentication token be returned, as it will be referenced by all
 * token providers to form the basis of the subject of any to-be-generated token, as well as the basis for any attributes
 * included in the to-be-generated token.
 *
 * The classes in this package are the interfaces and implementations specific to the rest-sts context.
 *
 */package org.forgerock.openam.sts.rest.token.validator;