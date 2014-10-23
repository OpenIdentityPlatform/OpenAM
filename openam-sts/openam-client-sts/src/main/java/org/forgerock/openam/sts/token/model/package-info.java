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


/**
 * This package contains classes that represent token types not represented in the wss4j or STS domain models, and the
 * classes which implement the xml and json marshalling.
 * These classes which represent tokens can serve as a type specifier for the generic types required in the AuthenticationHandler
 * and TokenAuthenticationRequestDispatcher. Regarding token marshalling:
 * tokens validated as part of a token transformation operation need json marshalling, to be specified in a
 * REST-STS invocation, and xml marshalling, to be transformed into the ReceivedToken (built around a DOM Element)
 * required by the CXF-STS. If the token type is produced as part of a token transformation, it will require XML marshalling
 * so that it can take a DOM Element representation required by the TokenProviderResponse, but then be turned into json
 * so that it can be returned from the REST-STS.
 *
 */
package org.forgerock.openam.sts.token.model;