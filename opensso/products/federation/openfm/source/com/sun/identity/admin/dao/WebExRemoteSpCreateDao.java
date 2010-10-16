/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: WebExRemoteSpCreateDao.java,v 1.2 2009/12/14 23:44:31 babysunil Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaConstants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.workflow.AddProviderToCOT;
import com.sun.identity.workflow.ImportSAML2MetaData;
import com.sun.identity.workflow.WorkflowException;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.JAXBException;

public class WebExRemoteSpCreateDao
        implements Serializable {

    public WebExRemoteSpCreateDao() {
    }

    public boolean createSamlv2RemoteSp(
            String realm,
            String entityId,
            String cot,
            String certContent,
            List attrMapping,
            boolean signAuthnreq,
            boolean sloNeeded) {

        StringBuffer stdMetadata = new StringBuffer();
        int begIndex = entityId.indexOf("/");
        int endIndex = entityId.indexOf(".");
        String siteurl = entityId.substring(begIndex + 2, endIndex);
        String certtoAdd = null;
        String authReqtosign = null;
        String respLoation = null;
        if (sloNeeded) {
             respLoation = "<SingleLogoutService Binding=\"urn:oasis:na" +
                    "mes:tc:SAML:2.0:bindings:HTTP-Redirect\" Locatio" +
                    "n=\"" + entityId+ "\" Response" +
                    "Location=\"" +entityId+ "\"/>";
        }

        if (signAuthnreq) {
            authReqtosign = "AuthnRequestsSigned=\"true\"\n";
        } else
        {
            authReqtosign = "AuthnRequestsSigned=\"false\"\n";
        }
        if (certContent != null && certContent.length() > 0) {
            String orig = null;
            if (certContent.startsWith("-")) {
                int begCertIndex = certContent.indexOf("CATE-----");
                int endCertIndex =
                        certContent.indexOf("-----END CERTIFICATE-----");
                orig = certContent.substring(begCertIndex + 9, endCertIndex);
            }
            if (orig != null && orig.length() > 0) {
                String headCertContent = "<KeyDescriptor use=\"sig" +
                        "ning\"> <ds:KeyInfo xmlns:ds=\"http://www.w3.org/20" +
                        "00/09/xmldsig#\"> <ds:X509Data> <ds:X509Certificate>";
                String tailCertContent = "</ds:X509Certificate> </ds:X509Dat" +
                        "a> </ds:KeyInfo> </KeyDescriptor>";
                certtoAdd = headCertContent + orig + tailCertContent;
            }
        }
        stdMetadata.append(
                "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
                "entityID=\"" + entityId + "\">\n" +
                "<SPSSODescriptor\n" + authReqtosign +
                "WantAssertionsSigned=\"true\"\n" +
                "protocolSupportEnumeration=\"urn:oasis:names:tc:SA" +
                "ML:2.0:protocol\">" + certtoAdd + respLoation + " <NameIDForm" +
                "at> urn:oasis:names:tc:SAM" +
                "L:1.1:nameid-format:unspecified </NameIDFormat> <NameID" +
                "Format>urn:oasis:names:tc:SAML:1.1:name" +
                "id-format:emailAddress</NameIDFormat> <NameIDFormat>urn:oas" +
                "is:names:tc:SAML:2.0:nameid-format:persistent</NameIDFor" +
                "mat> <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-for" +
                "mat:X509SubjectName</NameIDFormat> <AssertionCons" +
                "umerService index=\"0\" Binding=\"urn:oasis:name" +
                "s:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + entityId + "/dispatche" +
                "r/SAML2AuthService.do?site" +
                "url=" + siteurl + "\"/> </SPSSODescriptor> </EntityDescriptor> ");

        try {
            String extMetadata = createExtendedDataTemplate(
                    entityId, false);
            String[] results = ImportSAML2MetaData.importData(
                    realm, stdMetadata.toString(), extMetadata);
            realm = results[0];

        } catch (WorkflowException ex) {
            throw new RuntimeException(ex);
        }

        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException ex) {
                throw new RuntimeException(ex);
            }

        }

        if (!attrMapping.isEmpty()) {
            if (!addAttributeMapping(realm, entityId, attrMapping)) {
                return false;
            }
        }

        return true;
    }

    private boolean addAttributeMapping(
            String realm, String entityId, List attrMapping) {
        try {

            SAML2MetaManager manager = new SAML2MetaManager();
            EntityConfigElement config =
                    manager.getEntityConfig(realm, entityId);
            SPSSOConfigElement ssoConfig =
                    manager.getSPSSOConfig(realm, entityId);

            if (ssoConfig != null) {
                ObjectFactory objFactory = new ObjectFactory();
                AttributeType avp = objFactory.createAttributeElement();
                String key = SAML2Constants.ATTRIBUTE_MAP;
                avp.setName(key);
                avp.getValue().addAll(attrMapping);
                ssoConfig.getAttribute().add(avp);
            }
            manager.setEntityConfig(realm, config);

        } catch (SAML2MetaException ex) {
            throw new RuntimeException(ex);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }


    private String createExtendedDataTemplate(
            String entityID,
            boolean hosted) {
        StringBuffer buff = new StringBuffer();
        String strRemote = (hosted) ? "1" : "0";
        buff.append(
                "<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                "    xmlns:fm=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                "    hosted=\"" + strRemote + "\"\n" +
                "    entityID=\"" + entityID + "\">\n\n" +
                "    <SPSSOConfig>\n" +
                "    </SPSSOConfig>\n" +
                "</EntityConfig>\n");
        return buff.toString();
    }
}
