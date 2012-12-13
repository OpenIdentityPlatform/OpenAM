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
 * $Id: GetIDPSPPairingInCOT.java,v 1.3 2009/01/09 17:42:55 veiming Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Gets all IDP in a circle of trust.
 */
public class GetIDPSPPairingInCOT
    extends Task 
{
    public GetIDPSPPairingInCOT() {
    }

    public String execute(Locale locale, Map params)
        throws WorkflowException {
        String realm = getString(params, ParameterKeys.P_REALM);
        String cotName = getString(params, ParameterKeys.P_COT);
        
        List hostedIDP = getHostedIDP(realm, cotName);
        List hostedIDPMetaAlias = Collections.EMPTY_LIST;
        List remoteSP = getRemoteSP(realm, cotName);
        
        if (hostedIDP.isEmpty() || remoteSP.isEmpty()) {
            remoteSP = Collections.EMPTY_LIST;
        } else {
            hostedIDPMetaAlias = getHostedIDPMetaAlias(realm, hostedIDP);
        }
        
        List remoteIDP = getRemoteIDP(realm, cotName);
        List hostedSP = getHostedSP(realm, cotName);
        List hostedSPMetaAlias = Collections.EMPTY_LIST;
        
        if (remoteIDP.isEmpty() || hostedSP.isEmpty()) {
            remoteIDP = Collections.EMPTY_LIST;
        } else {
            hostedSPMetaAlias = getHostedSPMetaAlias(realm, hostedSP);
        }
        
        StringBuffer buff = new StringBuffer();
        buff.append(getArrayString("hostedidp", hostedIDPMetaAlias));
        buff.append(getArrayString("remoteidp", remoteIDP));
        buff.append(getArrayString("hostedsp", hostedSPMetaAlias));
        buff.append(getArrayString("remotesp", remoteSP));

        return buff.toString();
    }
    
    private List getHostedIDPMetaAlias(String realm, List hostedIDP) 
       throws  WorkflowException {
        try {
            List list = new ArrayList();
            SAML2MetaManager mgr = new SAML2MetaManager();
            for (Iterator i = hostedIDP.iterator(); i.hasNext();) {
                String e = (String) i.next();
                IDPSSOConfigElement cfg = mgr.getIDPSSOConfig(realm, e);
                list.add(e + "(" + cfg.getMetaAlias() + ")");
            }
            return list;
        } catch (SAML2MetaException ex) {
            throw new WorkflowException(ex.getMessage());
        }
    }

    private List getHostedSPMetaAlias(String realm, List hostedSP) 
       throws  WorkflowException {
        try {
            List list = new ArrayList();
            SAML2MetaManager mgr = new SAML2MetaManager();
            for (Iterator i = hostedSP.iterator(); i.hasNext();) {
                String e = (String) i.next();
                SPSSOConfigElement cfg = mgr.getSPSSOConfig(realm, e);
                list.add(e + "(" + cfg.getMetaAlias() + ")");
            }
            return list;
        } catch (SAML2MetaException ex) {
            throw new WorkflowException(ex.getMessage());
        }
    }

  
    private String getArrayString(String arrayName, List list) {
        StringBuffer buff = new StringBuffer();
        buff.append("{")
            .append(arrayName)
            .append("=");
        boolean first = true;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String e = (String)i.next();
            if (!first) {
                buff.append(",");
            } else {
                first = false;
            }
            try {
                buff.append(URLEncoder.encode(e, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                buff.append(e);
            }
        }
        buff.append("}");
        return buff.toString();
    }
    
    private List getHostedIDP(String realm, String cotName)
        throws WorkflowException {
        return getEntities(realm, cotName, true, true);
    }
    
    private List getRemoteIDP(String realm, String cotName)
        throws WorkflowException {
        return getEntities(realm, cotName, true, false);
    }
    
    
    private List getHostedSP(String realm, String cotName)
        throws WorkflowException {
        return getEntities(realm, cotName, false, true);
    }
    
    private List getRemoteSP(String realm, String cotName)
        throws WorkflowException {
        return getEntities(realm, cotName, false, false);
    }

    private List getEntities(
        String realm, 
        String cotName, 
        boolean bIDP, 
        boolean hosted
    ) throws WorkflowException {
        try {
            SAML2MetaManager mgr = new SAML2MetaManager();
            Set entities = getEntities(realm, cotName);
            List results = new ArrayList();

            for (Iterator i = entities.iterator(); i.hasNext();) {
                String entityId = (String) i.next();
                EntityConfigElement elm = mgr.getEntityConfig(realm, entityId);
                if (elm.isHosted() == hosted) {
                    EntityDescriptorElement desc = mgr.getEntityDescriptor(
                        realm, entityId);
                    
                    if (bIDP) {
                        if (SAML2MetaUtils.getIDPSSODescriptor(desc) != null) {
                            results.add(entityId);
                        }
                    } else {
                        if (SAML2MetaUtils.getSPSSODescriptor(desc) != null) {
                            results.add(entityId);
                        }
                    }
                }
            }
            return results;
        } catch (SAML2MetaException ex) {
            throw new WorkflowException(ex.getMessage());
        }
    }
    
    private Set getEntities(String realm, String cotName) 
        throws WorkflowException {
        try {
            CircleOfTrustManager mgr = new CircleOfTrustManager();
            return mgr.listCircleOfTrustMember(realm, cotName, 
                COTConstants.SAML2);
        } catch (COTException ex) {
            throw new WorkflowException(ex.getMessage());
        }
    }
}
