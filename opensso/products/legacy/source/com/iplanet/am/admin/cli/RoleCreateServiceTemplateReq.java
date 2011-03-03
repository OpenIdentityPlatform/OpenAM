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
 * $Id: RoleCreateServiceTemplateReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.io.PrintWriter;
import java.io.StringWriter;

class RoleCreateServiceTemplateReq extends AdminReq {

    private String serviceName = null;
                    

    /**
     * Constructs a new RoleCreateServiceTemplateReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    RoleCreateServiceTemplateReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription67") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }


    void addRoleServiceTmplReq(String svcName) {
        serviceName = svcName;
    }

    void process(AMStoreConnection dpConnection, SSOToken ssoToken)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("role") + " " + targetDN +
                "\n" + bundle.getString("createservicetemplates"));
        }

        AdminReq.writer.println(bundle.getString("role") + " " +
            targetDN + "\n" + bundle.getString("createservicetemplates"));

        try {
            ServiceSchemaManager ssm =
                getServiceSchemaManager(ssoToken, serviceName);

            if (ssm.getDynamicSchema() != null) {
                AMRole role = dpConnection.getRole(targetDN);
                doLog(serviceName, role,
                    AdminUtils.CREATE_SERVTEMPLATE_ATTEMPT);
                role.createTemplate(
                    AMTemplate.DYNAMIC_TEMPLATE, serviceName, null, 0);
                AdminReq.writer.println(serviceName);
//              doLog(serviceName, role, "create-servtemplate");
                doLog(serviceName, role, AdminUtils.CREATE_SERVTEMPLATE);
            } else {
                throw new AdminException(
                    bundle.getString("nodynamicschema") + ": "+ serviceName);
            }

        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}
