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
 * $Id: ConfigureGoogleApps.java,v 1.4 2009/05/07 21:35:06 asyhuang Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.workflow;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.bind.JAXBException;

/**
 ** Configure GoogleApps.
 **/
public class ConfigureGoogleApps
        extends Task {

    private static String nameidMapping =
            "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified=uid";

    public ConfigureGoogleApps() {
    }

    public String execute(Locale locale, Map params)
            throws WorkflowException {
        String domainIds = getString(params, ParameterKeys.P_DOMAIN_ID);
        String entityId = getString(params, ParameterKeys.P_IDP);
        String realm = getString(params, ParameterKeys.P_REALM);
        String cot = getString(params, ParameterKeys.P_COT);
        if (!(domainIds.length() > 0) || (domainIds == null)) {
            Object[] param = {domainIds};
            throw new WorkflowException("domain.is.empty", param);
        }
        updateIDPMeta(realm, entityId);

        StringTokenizer st = new StringTokenizer(domainIds, ",");
        while (st.hasMoreTokens()) {
            updateSPMeta(realm, cot, st.nextToken().trim());
        }

        Object[] param = {entityId};
        return MessageFormat.format(
                getMessage("google.apps.configured.success", locale), param);
    }

    private void updateIDPMeta(String realm, String entityId)
            throws WorkflowException {
        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();
            EntityConfigElement entityConfig =
                    samlManager.getEntityConfig(realm, entityId);
            IDPSSOConfigElement idpssoConfig =
                    samlManager.getIDPSSOConfig(realm, entityId);
            List attrList = idpssoConfig.getAttribute();
            if (idpssoConfig != null) {
                for (Iterator it = attrList.iterator(); it.hasNext();) {
                    AttributeElement avpnew = (AttributeElement) it.next();
                    String name = avpnew.getName();
                    if (name.equals("nameIDFormatMap")) {
                        for (Iterator itt = avpnew.getValue().listIterator();
                                itt.hasNext();) {
                            String temp = (String) itt.next();
                            if (temp.contains("unspecified")) {
                                itt.remove();
                            }
                        }
                        avpnew.getValue().add(0, nameidMapping);
                    }
                }
            }
            samlManager.setEntityConfig(realm, entityConfig);
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        }
    }

    private void updateSPMeta(String realm, String cot, String domainId)
            throws WorkflowException {

        String metadata = "<EntityDescriptor entityID=\"google.com/a/"
                + domainId + "\"" + " xmlns=\"urn"
                + ":oasis:names:tc:SAML:2.0:metadata\">"
                + "<SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:nam"
                + "es:tc:SAML:2.0:protocol\"> <NameIDFormat>urn:oasis:names:t"
                + "c:SAML:1.1:nameid-format:unspecified</NameIDFormat>"
                + "<AssertionConsumerService index=\"1\" Binding=\"urn:oasis:na"
                + "mes:tc:SAML:2.0:bindings:HTTP-POST\""
                + " Location=\"https://www.google.com/a/"
                + domainId + "/acs\" />"
                + "</SPSSODescriptor></EntityDescriptor>";

        String extendedMeta = null;
        try {
            EntityDescriptorElement e =
                SAML2MetaUtils.getEntityDescriptorElement(metadata);
            String eId = e.getEntityID();
            String metaAlias = generateMetaAliasForSP(realm);
            Map map = new HashMap();
            map.put(MetaTemplateParameters.P_SP, metaAlias);
            extendedMeta =
                    CreateSAML2HostedProviderTemplate.createExtendedDataTemplate(
                    eId, map, null, false);
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
    }
}
