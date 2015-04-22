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
 * The classes in this package relate to token validation in the context of WS-SecurityPolicy binding. They validate
 * the SupportingTokens specified in SecurityPolicy bindings. Note that OpenAMSessionAssertions are not handled here,
 * as they are a custom token implementation, and thus cannot be handled by the cxf/wss4j token validator plugin
 * framework. The classes in this package are the actual org.apache.ws.security.validate.Validator implementations
 * (usually via extensions of existing, standard token validators in the org.apache.ws.security.validate package), and
 * the factory interface/impl which is consulted to produce instances of the Validator implementations to plug into the
 * wss4j runtime when a soap-sts instance is exposed as a web-service.
 */
package org.forgerock.openam.sts.soap.token.validator.wss;