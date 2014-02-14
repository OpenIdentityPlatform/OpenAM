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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

/**
 * The classes in this package and those below are concerned with token validation. They support the token validation
 * associated with both SecurityPolicy enforcement (defined by the org.apache.ws.security.validate.Validator interface in wss4j),
 * and the STS token validation defined in WS-Trust, which delegates token validation to the wss4j Validator instances.
 *
 * In other words, the org.apache.cxf.sts.token.validator.TokenValidator implementations defined in the CXF-STS, and the
 * org.apache.ws.security.validate.Validator implementations are both integrated into the OpenAM REST authN context via the
 * classes in this package, and in the packages below. The actual token validation functionality in
 * org.apache.cxf.sts.token.validator.TokenValidator implementations delegate token validation to org.apache.ws.security.validate.Validator
 * instances.
 *
 * The integration of the org.apache.ws.security.validate.Validator instances into the OpenAM REST authN context occurs
 * via the {@linkAuthenticationHandler<T>} interface (T is a particular token type).
 * The fundamental concern of the single implementation of this interface, the AuthenticationHandlerImpl, is to direct
 * an appropriately-formatted http-request at the realm-, authIndexType-, and authIndexValue-qualified URL.
 * Each STS-deployment is associated with a realm, and each authenticated token-type is associated with a corresponding
 * authIndexType and authIndexValue, which allows the authentication of each token type to be directed at a distinct
 * authentication context. These two concerns,
 * 1. the population of a realm-, authIndexType-, and authIndexValue-qualified URL, and
 * 2. the writing of the appropriate token state to this URI (i.e. a UsernameToken sets its username and password in the
 * X-OpenAM-Username/Password, whereas an X509 token gets serialized in a POST body), are handled by two interfaces, the
 * TokenAuthenticationRequestDispatcher<T> and the AuthenticationUrlProvider respectively (and where T stands for a
 * particular token type). So the implementation of the AuthenticationHandler<T> is simple: it just
 * 1. pulls the URI from the AuthenticationUriProvider and
 * 2. gives this URI to the TokenAuthenticationRequestDispatcher and tells it to dispatch the request and
 * 3. puts the successfully authenticated token in the TokenCache, so it can be referenced (i.e. to obtain the corresponding Principal)
 * by later token creation actions.
 *
 * One TokenAuthenticationRequestDispatcher<T>, and AuthenticationHandler<T> are bound for
 * each to-be-validated token type.
 *
 * The AuthenticationUriProvider consults the STS-instance-specific mapping of token type to authN uri state, so does not
 * need to be a generic type.
 */
package org.forgerock.openam.sts.token.validator.wss;