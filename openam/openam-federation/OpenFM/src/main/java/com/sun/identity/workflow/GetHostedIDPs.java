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
 * $Id: GetHostedIDPs.java,v 1.2 2008/06/25 05:50:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GetHostedIDPs
    extends Task 
{
    public GetHostedIDPs() {
    }

    public String execute(Locale locale, Map params)
        throws WorkflowException {
        String realm = getString(params, ParameterKeys.P_REALM);
        String cot = getString(params, ParameterKeys.P_COT);
        try {
            CircleOfTrustManager cotMgr = new CircleOfTrustManager();
            Set entities = cotMgr.listCircleOfTrustMember(realm, cot, 
                COTConstants.SAML2);
            SAML2MetaManager mgr = new SAML2MetaManager();

            StringBuffer buff = new StringBuffer();
            boolean first = true;
            
            for (Iterator i = entities.iterator(); i.hasNext();) {
                String entityId = (String) i.next();
                EntityConfigElement elm = mgr.getEntityConfig(realm, entityId);
                // elm could be null due to OPENAM-269
                if (elm != null && elm.isHosted()) {
                    EntityDescriptorElement desc = mgr.getEntityDescriptor(
                        realm, entityId);
                    if (SAML2MetaUtils.getIDPSSODescriptor(desc) != null) {
                        if (first) {
                            first = false;
                        } else {
                            buff.append("|");
                        }

                        buff.append(entityId);
                    }
                }
            }

            return buff.toString();
        } catch (COTException e) {
            throw new WorkflowException(e.getMessage(), null);
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage(), null);
        }
    }
            
            
}
