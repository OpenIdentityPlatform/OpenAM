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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link DocumentBuilderProvider} that returns safe {@link javax.xml.parsers.DocumentBuilder} instances that are
 * protected from various entity expansion and remote DTD attacks.
 * <p/>
 * Note: most callers should wrap this in a {@link PerThreadDocumentBuilderProvider}.
 *
 * @see com.sun.identity.shared.xml.XMLUtils#getSafeDocumentBuilder(boolean)
 */
final class SafeDocumentBuilderProvider implements DocumentBuilderProvider {

    /**
     * Returns safe document builder instances as from {@link XMLUtils#getSafeDocumentBuilder(boolean)}.
     * {@inheritDoc}
     */
    public DocumentBuilder getDocumentBuilder(boolean validating) throws ParserConfigurationException {
        return XMLUtils.getSafeDocumentBuilder(validating);
    }
}
