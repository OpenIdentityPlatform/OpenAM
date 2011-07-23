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
 * $Id: RoleModifyServiceTemplateReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class RoleModifyServiceTemplateReq extends AdminReq {

    private Map ServiceTemplateReq = new HashMap();
    protected String serviceName = null;

    /**
     * Constructs a new RoleModifyServiceTemplateReq.
     * @param targetDN the Organization DN. 
     */        
    RoleModifyServiceTemplateReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Converts this object into a string.
     *
     * @return string equalivant of this object.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        prnWriter.println(AdminReq.bundle.getString("requestdescription68") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * adds the role dn's and its avPair Map to ServiceTemplateReq Map.
     *
     * @param RoleDN the DN of a group
     * @param avPair the Map which contains the attribute as key and value as
     *        value
     */        
    void addServiceTemplateReq(String service, Map avPair) {
        serviceName = service;
        ServiceTemplateReq = avPair;
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("role") + " " + targetDN + "\n" +
                bundle.getString("modifyservicetemplates") + "\n" +
                serviceName);
        }

        AdminReq.writer.println(bundle.getString("role") + " " + targetDN +
            "\n" + bundle.getString("modifyservicetemplates") +
            "\n" + serviceName);

        try {
            AMTemplate tmpl = getTemplate(dpConnection);
            doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE_ATTEMPT);
            tmpl.setAttributes(ServiceTemplateReq);
            tmpl.store();
//            doLog(tmpl, "modify-servtemplate");
            doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected AMTemplate getTemplate(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMRole role = dpConnection.getRole(targetDN);
            return role.getTemplate(serviceName, AMTemplate.DYNAMIC_TEMPLATE);
        } catch (AMException ame) {
            String ErrorCode = ame.getErrorCode();

            if (ErrorCode.equals("461")) {
                throw new AdminException(bundle.getString("templateNotExist") +
                    ": "+ serviceName);
            } else {
                throw new AdminException(ame);
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
