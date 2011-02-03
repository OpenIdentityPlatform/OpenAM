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
 * $Id: ConfigureSalesForceApps.java,v 1.2 2009/08/06 18:06:17 babysunil Exp $
 *
 */
package com.sun.identity.workflow;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeElement;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 ** Configure SalesForceApps.
 **/
public class ConfigureSalesForceApps
        extends Task {

    public ConfigureSalesForceApps() {
    }

    public String execute(Locale locale, Map params)
            throws WorkflowException {
        String entityId = getString(params, ParameterKeys.P_IDP);
        String realm = getString(params, ParameterKeys.P_REALM);
        String cot = getString(params, ParameterKeys.P_COT);
        List attrMapping = getAttributeMapping(params);
        if (attrMapping.isEmpty()) {
            Object[] param = {entityId};
            throw new WorkflowException("attributemapping.is.empty", param);
        }

        updateSPMeta(realm, cot, attrMapping);

        Object[] param = {entityId};
        return MessageFormat.format(
                getMessage("google.apps.configured.success", locale), param);
    }

    private void updateSPMeta(String realm, String cot, List attrMapping)
            throws WorkflowException {

        String metadata = "<EntityDescriptor entityID=\"https://saml.salesfor" +
                "ce.com\" xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"><SP" +
                "SSODescriptor AuthnRequestsSigned=\"false\" WantAssertionsSi" +
                "gned=\"false\" protocolSupportEnumeration=\"urn:oasis:name" +
                "s:tc:SAML:2.0:protocol\"> <NameIDFormat>urn:oasis:names:t" +
                "c:SAML:1.1:nameid-format:unspecified</NameIDFormat><NameIDF" +
                "ormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</N" +
                "ameIDFormat> <AssertionConsumerService index=\"1\" Bindin" +
                "g=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Locati" +
                "on=\"https://login.salesforce.com\"/></SPSSODescript" +
                "or></EntityDescriptor>";
        String extendedMeta = null;
        try {
            EntityDescriptorElement e =
                    ImportSAML2MetaData.getEntityDescriptorElement(metadata);
            String eId = e.getEntityID();
            String metaAlias = generateMetaAliasForSP(realm);
            Map map = new HashMap();
            map.put(MetaTemplateParameters.P_SP, metaAlias);
            extendedMeta =
                    createExtendedDataTemplate(
                    eId, false);
        } catch (SAML2MetaException ex) {
            throw new WorkflowException(ex.getMessage());
        } catch (JAXBException ex) {
            throw new WorkflowException(ex.getMessage());
        }
        String[] results = ImportSAML2MetaData.importData(
                realm, metadata, extendedMeta);
        String entityId = results[1];
        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException e) {
                throw new WorkflowException(e.getMessage());
            }
        }

        try {
            if (!attrMapping.isEmpty()) {
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
            }
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        } catch (JAXBException e) {
            throw new WorkflowException(e.getMessage());
        }

    }

    private String createExtendedDataTemplate(
            String entityID,
            boolean hosted) {

        StringBuffer buff = new StringBuffer();
        String strHosted = (hosted) ? "1" : "0";
        buff.append(
                "<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                "    xmlns:fm=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                "    hosted=\"" + strHosted + "\"\n" +
                "    entityID=\"" + entityID + "\">\n\n" +
                "    <SPSSOConfig>\n" +
                "    </SPSSOConfig>\n" +
                "</EntityConfig>\n");
        return buff.toString();
    }
}
