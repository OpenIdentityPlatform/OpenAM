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
 * $Id: GetCircleOfTrusts.java,v 1.4 2009/01/09 17:42:55 veiming Exp $
 *
 */
/*
 * Portions Copyrighted 2014 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd.
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;

import org.forgerock.openam.utils.StringUtils;

public class GetCircleOfTrusts
    extends Task
{
    public GetCircleOfTrusts() {
    }

    /**
     * Returns Circle of trust of a given realm or extended metadata.
     *
     * @param locale Locale of the request.
     * @param params Map of creation parameters.
     */
    public String execute(Locale locale, Map params)
        throws WorkflowException {
        String realm = getString(params, ParameterKeys.P_REALM);
        if (realm == null) {
            String extendedMetaData = getString(params,
                ParameterKeys.P_EXTENDED_DATA);
            if (extendedMetaData != null) {
                realm = getRealmFromExtData(getContent(
                    extendedMetaData, locale));
            }
        }

        if (realm == null) {
            throw new WorkflowException("invalid-metaalias-slash", null);
        }

        try {
            CircleOfTrustManager mgr = new CircleOfTrustManager();
            Set cots = mgr.getAllCirclesOfTrust(realm);
            StringBuffer buff = new StringBuffer();

            if ((cots != null) && !cots.isEmpty()) {
                boolean first = true;
                for (Iterator i = cots.iterator(); i.hasNext(); ) {
                    String c = (String)i.next();
                    if (first) {
                        first = false;
                    } else {
                        buff.append("|");
                    }
                    try {
                        buff.append(StringUtils.encodeURIComponent(c, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        buff.append(c);
                    }
                }
            }

            return buff.toString();
        } catch (COTException e) {
            throw new WorkflowException("invalid-extended-data-cot", null);
        }
    }

    private String getRealmFromExtData(String xml)
        throws WorkflowException {
        String realm = null;
        try {
            Object obj = SAML2MetaUtils.convertStringToJAXB(xml);
            EntityConfigElement configElt =
                (obj instanceof EntityConfigElement) ?
                (EntityConfigElement)obj : null;
            if (configElt != null && configElt.isHosted()) {
                List config =
                configElt.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                if (!config.isEmpty()) {
                    BaseConfigType bConfig = (BaseConfigType)
                        config.iterator().next();
                    realm = SAML2MetaUtils.getRealmByMetaAlias(
                        bConfig.getMetaAlias());
                }
            }
        } catch (JAXBException e) {
            throw new WorkflowException("invalid-extended-data-cot", null);
        }
        return realm;
    }
}
