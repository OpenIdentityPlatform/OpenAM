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
 * $Id: WebExUrlsDao.java,v 1.2 2009/12/14 23:44:31 babysunil Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.console.federation.SAMLv2AuthContexts;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SingleLogoutServiceElement;
import com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WebExUrlsDao implements Serializable {

    public static final String IDP_AUTHN_CONTEXT_CLASS_REF_MAPPING =
            "idpAuthncontextClassrefMapping";

    public String getSingleLogoutUrl(String realm, String entityId) {

        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();

            IDPSSODescriptorElement idpssoDescriptor =
                    samlManager.getIDPSSODescriptor(realm, entityId);
            String signoutPageURL = null;

            if (idpssoDescriptor != null) {

                List signoutList = idpssoDescriptor.getSingleLogoutService();

                for (int i = 0; i < signoutList.size(); i++) {
                    SingleLogoutServiceElement signElem =
                            (SingleLogoutServiceElement) signoutList.get(i);
                    String tmp = signElem.getBinding();
                    if (tmp.contains("HTTP-Redirect")) {
                        signoutPageURL = signElem.getLocation();

                    }
                }
            }

            return signoutPageURL;

        } catch (SAML2MetaException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSingleSignOnUrl(String realm, String entityId) {

        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();

            IDPSSODescriptorElement idpssoDescriptor =
                    samlManager.getIDPSSODescriptor(realm, entityId);
            String signinPageURL = null;

            if (idpssoDescriptor != null) {

                List signonList = idpssoDescriptor.getSingleSignOnService();

                for (int i = 0; i < signonList.size(); i++) {
                    SingleSignOnServiceElement signElem =
                            (SingleSignOnServiceElement) signonList.get(i);
                    String tmp = signElem.getBinding();
                    if (tmp.contains("HTTP-Redirect")) {
                        signinPageURL = signElem.getLocation();

                    }
                }
            }

            return signinPageURL;

        } catch (SAML2MetaException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAuthnContext(String realm, String entityId) {

        SAMLv2AuthContexts cxt = new SAMLv2AuthContexts();
        String name = null;
        try {
            SAML2MetaManager saml2MetaManager = new SAML2MetaManager();
            Map map = new HashMap();

            BaseConfigType idpConfig =
                    saml2MetaManager.getIDPSSOConfig(realm, entityId);
            if (idpConfig != null) {
                map = SAML2MetaUtils.getAttributes(idpConfig);
            } else {
                throw new RuntimeException("invalid entity name");
            }
            List list = (List) map.get(IDP_AUTHN_CONTEXT_CLASS_REF_MAPPING);

            for (int i = 0; i < list.size(); i++) {
                String tmp = (String) list.get(i);
                int index = tmp.lastIndexOf("|");
                tmp = tmp.substring(0, index);
                index = tmp.lastIndexOf("|");
                tmp = tmp.substring(0, index);
                index = tmp.indexOf("|");
                name = tmp.substring(0, index);
            }
            return name;
        } catch (SAML2MetaException e) {
            throw new RuntimeException(e);
        }
    }

    public String getIdpinitTestUrl(
            String realm,
            String entityId,
            String spName) {
        StringBuilder testUrl = new StringBuilder();
        String metaAlias = null;
         try {
            SAML2MetaManager saml2MetaManager = new SAML2MetaManager();

            BaseConfigType idpssoConfig =
                    saml2MetaManager.getIDPSSOConfig(realm, entityId);
           if (idpssoConfig != null) {
                    BaseConfigType baseConfig = (BaseConfigType)idpssoConfig;
                    metaAlias = baseConfig.getMetaAlias();
                }
            if (null != metaAlias && metaAlias.length() > 0) {
              testUrl.append(entityId).append("/idpssoinit?metaAlias=").
                  append(metaAlias).append("&spEntityID=").append(spName).
                  append("&RelayState=").append(spName);
            }

            return testUrl.toString();
        } catch (SAML2MetaException e) {
            throw new RuntimeException(e);
        }

    }
}
