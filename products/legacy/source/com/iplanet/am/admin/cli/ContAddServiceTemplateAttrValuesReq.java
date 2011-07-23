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
 * $Id: ContAddServiceTemplateAttrValuesReq.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;

import java.util.Iterator;
import java.util.Set;

class ContAddServiceTemplateAttrValuesReq
    extends AddServiceTemplateAttrValuesReq
{
    /**
     * Constructs a new instance.
     *
     * @param targetDN distinguished name of Organizational Unit.
     */        
    ContAddServiceTemplateAttrValuesReq(String targetDN) {
        super(targetDN);
    }

    String getDescriptionString() {
        return "requestdescription79";
    }
        
    void modifyServiceTemplates(
        AMStoreConnection dpConnection,
        String dn
    ) throws AdminException
    {
        AMOrganizationalUnit ou = null;
        AMTemplate tmpl = null;

        try {
            ou = dpConnection.getOrganizationalUnit(dn);
            tmpl = getTemplate(ou);

            if (tmpl != null) {
                addAttributeValues(tmpl);

                if (roleTemplate) {
                    Set roles = ou.getRoles(AMConstants.SCOPE_ONE);

                    for (Iterator iter = roles.iterator(); iter.hasNext();) {
                        String roleDN = (String) iter.next();
                        AMRole role = dpConnection.getRole(roleDN);
                        tmpl = getTemplate(role);

                        if (tmpl != null) {
                            addAttributeValues(tmpl);
                        }
                    }
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        String msg = bundle.getString("container") + " " + targetDN + "\n" +
            bundle.getString("addservicetemplateattributevalues") + "\n" +
            serviceName;

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(msg);
        }

        writer.println(msg);

        try {
            if (scope.equalsIgnoreCase("SCOPE_ONE")) {
                modifyServiceTemplates(dpConnection, targetDN);
            } else {
                AMOrganizationalUnit ou =
                    dpConnection.getOrganizationalUnit(targetDN);
                Set ous = ou.getSubOrganizationalUnits(AMConstants.SCOPE_SUB);

                for (Iterator iter = ous.iterator(); iter.hasNext(); ) {
                    modifyServiceTemplates(dpConnection, (String)iter.next());
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
