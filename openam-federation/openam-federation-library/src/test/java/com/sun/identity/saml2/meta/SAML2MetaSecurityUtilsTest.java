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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.saml2.meta;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import org.testng.annotations.Test;
import org.w3c.dom.Document;


public class SAML2MetaSecurityUtilsTest {

    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";

    @Test
    public void testVerifySignature() throws SAML2Exception {

        Document doc = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT),
                SAML2MetaUtils.debug);
        // The keystore properties required to bootstrap the underlying key provider class are setup in the POM
        SAML2MetaSecurityUtils.verifySignature(doc);
    }
}
