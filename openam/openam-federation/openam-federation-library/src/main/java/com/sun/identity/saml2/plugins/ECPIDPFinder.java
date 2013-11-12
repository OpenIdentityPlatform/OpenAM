/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ECPIDPFinder.java,v 1.2 2008/06/25 05:47:51 qcheng Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import java.util.List;
import java.util.Map;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.protocol.AuthnRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class <code>ECPIDPFinder</code> is used to find a list of 
 * preferred Identity Providers to be placed in the child element "IDPList" of
 * ECP Request element.
 */ 
public class ECPIDPFinder implements SAML2IDPFinder {

    /**
     * Returns a list of preferred IDP providerID's.
     * @param authnRequest original authnrequest
     * @param hostProviderID hosted providerID.
     * @param realm Realm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return a list of IDP providerID's or null if not found.
     * @exception SAML2Exception if error occurs. 
     */
    public List getPreferredIDP(AuthnRequest authnRequest,
        String hostProviderID, String realm, HttpServletRequest request,
        HttpServletResponse response) throws SAML2Exception {

        SPSSOConfigElement spssoconfig = SAML2Utils.getSAML2MetaManager()
            .getSPSSOConfig(realm, hostProviderID);
        Map attributes = SAML2MetaUtils.getAttributes(spssoconfig);
        List idps = (List)attributes.get(SAML2Constants.ECP_REQUEST_IDP_LIST);
        if ((idps == null) || (idps.isEmpty())) {
            return null;
        }
        return idps;
    }
}
