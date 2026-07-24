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
package com.sun.identity.liberty.ws.disco.common;

import com.sun.identity.liberty.ws.disco.jaxb.EncryptedResourceIDType;
import com.sun.identity.shared.xml.XMLUtils;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class DiscoUtilsTest {

    /**
     * The raw {@code EncryptedResourceIDType} has no {@code @XmlRootElement}, so DST request
     * handling must marshal it through the {@code EncryptedResourceIDElement} wrapper; marshalling
     * the bare type throws {@code MarshalException}.
     */
    public void encryptedResourceIDMarshalsViaElementWrapper() throws Exception {
        EncryptedResourceIDType encID = DiscoUtils.getDiscoFactory().createEncryptedResourceIDType();

        Document doc = XMLUtils.newDocument();
        DiscoUtils.getDiscoMarshaller().marshal(
                DiscoUtils.getDiscoFactory().createEncryptedResourceIDElement(encID), doc);

        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("EncryptedResourceID");
        assertThat(doc.getDocumentElement().getNamespaceURI()).isEqualTo("urn:liberty:disco:2003-08");
    }
}
