/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CreateRemoteSP.java,v 1.9 2009/04/09 06:53:43 asyhuang Exp $
 *
 */

 /*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.workflow;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 * Creates Remote Service Provider.
 */
public class CreateRemoteSP
        extends Task {

    public CreateRemoteSP() {
    }

    /**
     * Creates remote service provider.
     *
     * @param locale Locale of the request.
     * @param params Map of creation parameters.
     */
    @Override
    public String execute(Locale locale, Map params)
            throws WorkflowException {
        validateParameters(params);
        String realm = getString(params, ParameterKeys.P_REALM);
        String metadataFile = getString(params, ParameterKeys.P_META_DATA);
        String metadata = getContent(metadataFile, locale);
        String extendedMeta = null;
        List attrMapping = getAttributeMapping(params);

        if (!attrMapping.isEmpty()) {
            try {
                EntityDescriptorElement e =
                        SAML2MetaUtils.getEntityDescriptorElement(metadata);
                String eId = e.getEntityID();
                extendedMeta =
                        createExtendedDataTemplate(
                        eId, false);
            } catch (SAML2MetaException ex) {
                throw new WorkflowException(ex.getMessage());
            } catch (JAXBException ex) {
                throw new WorkflowException(ex.getMessage());
            }
        }

        String[] results = ImportSAML2MetaData.importData(
                realm, metadata, extendedMeta);
        String entityId = results[1];
        String cot = getString(params, ParameterKeys.P_COT);
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

        return getMessage("sp.configured", locale);
    }

    private void validateParameters(Map params)
            throws WorkflowException {
        String metadata = getString(params, ParameterKeys.P_META_DATA);
        if ((metadata == null) || (metadata.trim().length() == 0)) {
            throw new WorkflowException("meta-data-required", null);
        }
        String realm = getString(params, ParameterKeys.P_REALM);
        if ((realm == null) || (realm.trim().length() == 0)) {
            throw new WorkflowException("missing-realm", null);
        }
    }

    private String createExtendedDataTemplate(
            String entityID,
            boolean hosted) {

        StringBuilder buff = new StringBuilder();
        String strHosted = (hosted) ? "1" : "0";
        buff.append("<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\"\n");
        buff.append("    xmlns:fm=\"urn:sun:fm:SAML:2.0:entityconfig\"\n");
        buff.append("    hosted=\"").append(strHosted).append("\"\n");
        buff.append("    entityID=\"").append(entityID).append("\">\n\n");
        buff.append("    <SPSSOConfig>\n");
        buff.append("    </SPSSOConfig>\n");
        buff.append("</EntityConfig>\n");

        return buff.toString();
    }
}
