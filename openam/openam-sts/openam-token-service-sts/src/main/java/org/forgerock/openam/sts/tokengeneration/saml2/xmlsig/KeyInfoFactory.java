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

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;

/**
 * This interface defines the concern around obtaining the KeyInfo element in the SubjectConfirmationData
 * element for SAML2 assertions issued with HolderOfKey SubjectConfirmation
 */
public interface KeyInfoFactory {
    /**
     *
     * @param recipientCert The cert, passed in the invocation to the TokenGenerationService, for which KeyInfo will
     *                      be generated for inclusion in the SubjectConfirmationData for HoK assertions.
     * @return The KeyInfo Element
     * @throws ParserConfigurationException
     * @throws XMLSecurityException
     */
    public Element generatePublicKeyInfo(X509Certificate recipientCert) throws ParserConfigurationException, XMLSecurityException;
}
