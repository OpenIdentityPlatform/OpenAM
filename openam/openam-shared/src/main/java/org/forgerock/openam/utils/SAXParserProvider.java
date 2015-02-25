/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.utils;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

/**
 * Abstract factory interface for constructing {@link javax.xml.parsers.SAXParser} instances. Implementations may
 * configure and cache instances for performance.
 *
 * @since 12.0.0
 */
public interface SAXParserProvider {

    /**
     * Gets a pre-configured {@link SAXParser} from the underlying implementation. Note: SAXParser instances
     * are not thread-safe and so should not shared between threads. Multiple calls to this method on the same
     * provider from the same thread <em>may</em> return the same instance, but are not required to.
     *
     * @param validating Whether the returned document builder should perform XML validation or not.
     * @return a pre-configured SAX parser.
     * @throws ParserConfigurationException if there is an error configuring the parser.
     * @throws SAXException if an error occurs initialising the SAX parser.
     */
    SAXParser getSAXParser(boolean validating) throws ParserConfigurationException, SAXException;
}
