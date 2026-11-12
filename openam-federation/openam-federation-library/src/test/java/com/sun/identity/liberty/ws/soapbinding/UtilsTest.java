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
package com.sun.identity.liberty.ws.soapbinding;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingElement;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingType;
import com.sun.identity.shared.xml.XMLUtils;
import org.testng.annotations.Test;

@Test
public class UtilsTest {

    /**
     * Unlike the metadata utilities, the Liberty ID-WSF unmarshallers stay lenient: JAXB 3 drops an
     * unexpected element of a received message, where JAXB 1 rejected the message.
     */
    public void unexpectedElementIsDropped() throws Exception {
        String offering = "<ResourceOffering xmlns=\"urn:liberty:disco:2003-08\">"
                + "<ResourceID>https://idp.example.com/resource</ResourceID>"
                + "<ServiceInstance><ServiceType>urn:liberty:id-sis-pp:2003-08</ServiceType>"
                + "<ProviderID>https://idp.example.com</ProviderID>"
                + "<Description><SecurityMechID>urn:liberty:security:2003-08:null:null</SecurityMechID>"
                + "<Endpoint>https://idp.example.com/Liberty/idpp</Endpoint></Description>"
                + "</ServiceInstance>"
                + "<Bogus/>"
                + "</ResourceOffering>";

        Object jaxb = Utils.convertElementToJAXB(
                XMLUtils.toDOMDocument(offering, Utils.debug).getDocumentElement());

        assertThat(jaxb).isInstanceOf(ResourceOfferingElement.class);
        ResourceOfferingType value = ((ResourceOfferingElement) jaxb).getValue();
        assertThat(value.getResourceID().getValue()).isEqualTo("https://idp.example.com/resource");
        assertThat(value.getServiceInstance().getProviderID()).isEqualTo("https://idp.example.com");
    }
}
