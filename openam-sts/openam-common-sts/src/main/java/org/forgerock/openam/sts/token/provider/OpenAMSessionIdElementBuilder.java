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

package org.forgerock.openam.sts.token.provider;

import org.forgerock.openam.sts.TokenMarshalException;
import org.w3c.dom.Element;

/**
 * Tokens are always represented as XML elements. This interface defines the functionality to create an XML element
 * encapsulating the OpenAM sessionId.
 */
public interface OpenAMSessionIdElementBuilder {
    Element buildOpenAMSessionIdElement(String sessionId) throws TokenMarshalException;
}
