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
 * $Id: CreateHostedIDP.java,v 1.9 2008/06/25 05:50:01 qcheng Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates Hosted Identity Provider.
 */
public class CreateHostedIDP
    extends Task {
    public CreateHostedIDP() {
    }

    /**
     * Creates hosted identity provider.
     *
     * @param locale Locale of the Request
     * @param params Map of creation parameters.
     */
    public String execute(Locale locale, Map params)
        throws WorkflowException {
        validateParameters(params);
        String metadataFile = getString(params, ParameterKeys.P_META_DATA);
        boolean hasMetaData = (metadataFile != null) &&
            (metadataFile.trim().length() > 0);
        String metadata = null;
        String extendedData = null;

        if (hasMetaData) {
            String extendedDataFile = getString(params,
                ParameterKeys.P_EXTENDED_DATA);
            metadata = getContent(metadataFile, locale);
            extendedData = getContent(extendedDataFile, locale);
        } else {
            String entityId = getString(params, ParameterKeys.P_ENTITY_ID);
            String metaAlias = generateMetaAliasForIDP(getString(params,
                ParameterKeys.P_REALM));
            Map map = new HashMap();
            map.put(MetaTemplateParameters.P_IDP, metaAlias);
            map.put(MetaTemplateParameters.P_IDP_E_CERT,
                getString(params, ParameterKeys.P_IDP_E_CERT));
            map.put(MetaTemplateParameters.P_IDP_S_CERT,
                getString(params, ParameterKeys.P_IDP_S_CERT));

            try {
                metadata = CreateSAML2HostedProviderTemplate.
                    buildMetaDataTemplate(entityId, map, getRequestURL(params));
                extendedData = CreateSAML2HostedProviderTemplate.
                    createExtendedDataTemplate(entityId, map,
                    getRequestURL(params));
            } catch (SAML2MetaException e) {
                return e.getMessage();
            }
        }

        String[] results = ImportSAML2MetaData.importData(
            null, metadata, extendedData);
        String realm = results[0];
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
            List attrMapping = getAttributeMapping(params);
            if (!attrMapping.isEmpty()) {
                SAML2MetaManager manager = new SAML2MetaManager();
                EntityConfigElement config =
                    manager.getEntityConfig(realm, entityId);
                IDPSSOConfigElement ssoConfig =
                    manager.getIDPSSOConfig(realm, entityId);

                Map attribConfig = SAML2MetaUtils.getAttributes(ssoConfig);
                List mappedAttributes = (List)attribConfig.get(
                    SAML2Constants.ATTRIBUTE_MAP);
                mappedAttributes.addAll(attrMapping);
                manager.setEntityConfig(realm, config);
            }
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        }
        try {
           return getMessage("idp.configured", locale) + "|||realm=" + realm +
                "&entityId=" + URLEncoder.encode(entityId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new WorkflowException(e.getMessage());
        }
    }

    private void validateParameters(Map params)
        throws WorkflowException {
        String metadata = getString(params, ParameterKeys.P_META_DATA);
        boolean hasMetaData = (metadata != null) &&
            (metadata.trim().length() > 0);
        String extendedData = getString(params, ParameterKeys.P_EXTENDED_DATA);
        boolean hasExtendedData = (extendedData != null) &&
            (extendedData.trim().length() > 0);

        if ((hasMetaData && !hasExtendedData) ||
            (!hasMetaData && hasExtendedData)
            ) {
            throw new WorkflowException("both-meta-extended-data-required",
                null);
        }
        if ((params.size() == 3) &&
            params.containsKey(ParameterKeys.P_META_DATA) &&
            params.containsKey(ParameterKeys.P_EXTENDED_DATA) &&
            !hasMetaData && !hasExtendedData
            ) {
            throw new WorkflowException("both-meta-extended-data-required",
                null);
        }

        String cotname = getString(params, ParameterKeys.P_COT);
        if ((cotname == null) || (cotname.trim().length() == 0)) {
            throw new WorkflowException("missing-cot", null);
        }

        if (!hasMetaData && !hasExtendedData) {
            String realm = getString(params, ParameterKeys.P_REALM);
            if ((realm == null) || (realm.trim().length() == 0)) {
                throw new WorkflowException("missing-realm", null);
            }

            String entityId = getString(params, ParameterKeys.P_ENTITY_ID);
            if ((entityId == null) || (entityId.trim().length() == 0)) {
                throw new WorkflowException("missing-entity-id", null);
            }
        }
    }
}
