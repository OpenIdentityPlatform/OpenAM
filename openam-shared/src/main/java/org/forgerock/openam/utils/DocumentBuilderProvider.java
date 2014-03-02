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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Abstract factory interface for supplying configured {@link javax.xml.parsers.DocumentBuilder} instances. The
 * underlying implementation may cache or pool instances for performance.
 * <p/>
 * Note: usually you will want to use the {@link PerThreadDocumentBuilderProvider} wrapped around a
 * {@link SafeDocumentBuilderProvider}.
 */
public interface DocumentBuilderProvider {
    /**
     * Gets a pre-configured {@link DocumentBuilder} from the underlying implementation. Note: DocumentBuilder instances
     * are not thread-safe and so should not shared between threads. Multiple calls to this method on the same
     * provider from the same thread <em>may</em> return the same instance, but are not required to.
     *
     * @param validating Whether the returned document builder should perform XML validation or not.
     * @return a pre-configured document builder.
     * @throws ParserConfigurationException if there is an error configuring the document builder.
     */
    DocumentBuilder getDocumentBuilder(boolean validating) throws ParserConfigurationException;
}
