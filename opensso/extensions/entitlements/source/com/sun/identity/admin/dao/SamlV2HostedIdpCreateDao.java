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
 * $Id: SamlV2HostedIdpCreateDao.java,v 1.7 2009/07/13 23:22:02 asyhuang Exp $
 */
package com.sun.identity.admin.dao;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.workflow.AddProviderToCOT;
import com.sun.identity.workflow.CreateSAML2HostedProviderTemplate;
import com.sun.identity.workflow.ImportSAML2MetaData;
import com.sun.identity.workflow.MetaTemplateParameters;
import com.sun.identity.workflow.WorkflowException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SamlV2HostedIdpCreateDao
        implements Serializable {

    public SamlV2HostedIdpCreateDao() {
    }

    public boolean createSamlv2HostedIdp(
            String realm,
            String entityId,
            String cot,
            String key,
            List attrMapping) {

        String metadata = null;
        String extendedData = null;

        String metaAlias = generateMetaAliasForIDP(realm);
        if (metaAlias == null) {
            return false;
        }
        Map map = new HashMap();
        map.put(MetaTemplateParameters.P_IDP, metaAlias);
        map.put(MetaTemplateParameters.P_IDP_S_CERT, key);

        try {
            metadata =
                    CreateSAML2HostedProviderTemplate.buildMetaDataTemplate(
                    entityId, map, SamlV2CreateSharedDao.getInstance().getRequestURL());
            extendedData =
                    CreateSAML2HostedProviderTemplate.createExtendedDataTemplate(
                    entityId, map, SamlV2CreateSharedDao.getInstance().getRequestURL());
        } catch (SAML2MetaException ex) {
            Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        try {
            String[] results = ImportSAML2MetaData.importData(null, metadata, extendedData);
        } catch (WorkflowException ex) {
            Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException ex) {
                Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        if (!attrMapping.isEmpty()) {
            if (!addAttributeMapping(realm, entityId, attrMapping)) {
                return false;
            }
        }
        return true;
    }

    public boolean importSamlv2HostedIdp(
            String cot,
            String stdMetadata,
            String extMetadata,
            List attrMapping) {
        String realm = null;
        String entityId = null;

        try {
            String[] results = ImportSAML2MetaData.importData(null, stdMetadata, extMetadata);
            realm = results[0];
            entityId = results[1];
        } catch (WorkflowException ex) {
            Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException ex) {
                Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        if (!attrMapping.isEmpty()) {
            if (!addAttributeMapping(realm, entityId, attrMapping)) {
                return false;
            }
        }
        return true;
    }

    private boolean addAttributeMapping(String realm, String entityId, List attrMapping) {
        try {

            SAML2MetaManager manager = new SAML2MetaManager();

            EntityConfigElement config =
                    manager.getEntityConfig(realm, entityId);
            IDPSSOConfigElement ssoConfig =
                    manager.getIDPSSOConfig(realm, entityId);

            Map attribConfig = SAML2MetaUtils.getAttributes(ssoConfig);
            List mappedAttributes = (List) attribConfig.get(
                    SAML2Constants.ATTRIBUTE_MAP);
            mappedAttributes.addAll(attrMapping);
            manager.setEntityConfig(realm, config);

        } catch (SAML2MetaException ex) {
            Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private String generateMetaAliasForIDP(String realm) {
        try {
            Set metaAliases = new HashSet();
            SAML2MetaManager mgr = new SAML2MetaManager();
            metaAliases.addAll(
                    mgr.getAllHostedIdentityProviderMetaAliases(realm));
            metaAliases.addAll(
                    mgr.getAllHostedServiceProviderMetaAliases(realm));
            String metaAliasBase = (realm.equals("/")) ? "/idp" : realm + "/idp";
            String metaAlias = metaAliasBase;
            int counter = 1;

            while (metaAliases.contains(metaAlias)) {
                metaAlias = metaAliasBase + Integer.toString(counter);
                counter++;
            }
            return metaAlias;
        } catch (SAML2MetaException ex) {
            //throw new RuntimeException(e);
            Logger.getLogger(SamlV2HostedIdpCreateDao.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}

