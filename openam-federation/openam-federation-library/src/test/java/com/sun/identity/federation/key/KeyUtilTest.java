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
package com.sun.identity.federation.key;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import org.testng.annotations.Test;

@Test
public class KeyUtilTest {

    /**
     * KeyInfo is mixed content, so indented ID-FF metadata unmarshals to a content list that starts
     * with a whitespace String; the X509Data element must be found by type, not by index.
     */
    public void certIsReadFromIndentedKeyInfo() throws Exception {
        EntityDescriptorElement entity;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("idff-sp-metadata-indented-keyinfo.xml")) {
            entity = (EntityDescriptorElement) IDFFMetaUtils.convertNodeToJAXB(
                    XMLUtils.toDOMDocument(is, FSUtils.debug));
        }
        SPDescriptorType spDescriptor = IDFFMetaUtils.getSPDescriptor(entity);

        X509Certificate cert = KeyUtil.getCert(spDescriptor, "encryption");

        assertThat(cert).isNotNull();
        assertThat(cert.getSubjectX500Principal().getName()).contains("CN=login.slub-dresden.de");
    }
}
