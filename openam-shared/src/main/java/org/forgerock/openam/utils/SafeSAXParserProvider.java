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

import org.forgerock.util.xml.XMLUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

/**
 * {@link SAXParserProvider} that delegates to {@link org.forgerock.util.xml.XMLUtils#getSafeSAXParser(boolean)} to
 * get SAXParser instances that are configured to avoid various entity expansion and remote DTD attacks.
 *
 * @since 12.0.0
 */
class SafeSAXParserProvider implements SAXParserProvider {
    /**
     * Delegates to {@link XMLUtils#getSafeSAXParser(boolean)} to get parser instances.
     *
     * @param validating Whether the returned document builder should perform XML validation or not.
     * @return a new parser instance.
     * @throws ParserConfigurationException if an error occurs configuring the parser.
     * @throws SAXException if an error occurs instantiating the parser.
     */
    public final SAXParser getSAXParser(boolean validating) throws ParserConfigurationException, SAXException {
        return XMLUtils.getSafeSAXParser(validating);
    }
}
