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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.federation.meta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.shared.xml.XMLUtils;
import jakarta.xml.bind.JAXBException;
import org.testng.annotations.Test;

@Test
public class IDFFMetaUtilsTest {

    /**
     * The JAXB 1 runtime rejected metadata with an unexpected element; JAXB 3 drops the element
     * unless the unmarshaller fails on errors. Here the KeyDescriptor is in the wrong namespace.
     */
    public void unexpectedElementIsRejected() throws Exception {
        String metadata = "<EntityDescriptor xmlns=\"urn:liberty:metadata:2003-08\""
                + " providerID=\"https://sp.example.com/idff\">"
                + "<SPDescriptor protocolSupportEnumeration=\"urn:liberty:iff:2003-08\">"
                + "<md:KeyDescriptor xmlns:md=\"urn:liberty:metadata:wrong\" use=\"signing\"/>"
                + "<SoapEndpoint>https://sp.example.com/SOAPReceiver/metaAlias/sp</SoapEndpoint>"
                + "<AssertionConsumerServiceURL id=\"1\" isDefault=\"true\">"
                + "https://sp.example.com/AssertionConsumerService/metaAlias/sp</AssertionConsumerServiceURL>"
                + "<AuthnRequestsSigned>false</AuthnRequestsSigned>"
                + "</SPDescriptor></EntityDescriptor>";

        assertThatThrownBy(() -> IDFFMetaUtils.convertStringToJAXB(metadata))
                .isInstanceOf(JAXBException.class);
        assertThatThrownBy(() -> IDFFMetaUtils.convertNodeToJAXB(XMLUtils.toDOMDocument(metadata, FSUtils.debug)))
                .isInstanceOf(JAXBException.class);
    }
}
