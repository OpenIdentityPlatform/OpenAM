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
 * $Id: OrgGetServiceTemplateReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class OrgGetServiceTemplateReq extends AdminReq {
    protected String serviceName;
    protected int schemaType;

    /**
     * Constructs a new OrgGetServiceTemplateReq.
     *
     * @param targetDN the Organization DN. 
     */        
    OrgGetServiceTemplateReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription69") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void setServiceNameSchemaType(String service, String schemaType)
         throws AdminException
    {
        if ((service == null) || (service.length() == 0) ||
            (schemaType == null) || (schemaType.length() == 0)
        ) {
            throw new UnsupportedOperationException(
                bundle.getString("unsupportedSchemaType"));
             }

        serviceName = service;
        setSchemaType(schemaType);
    }

    protected void setSchemaType(String schemaType)
        throws AdminException
    {
        this.schemaType = -1;

        if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_DYNAMIC)) {
            this.schemaType = AMTemplate.DYNAMIC_TEMPLATE;
        } else if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_ORGANIZATION)) {
            this.schemaType = AMTemplate.ORGANIZATION_TEMPLATE;
        } else {
            throw new UnsupportedOperationException(
                bundle.getString("unsupportedSchemaType"));
        }
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString(getType()) + " " + targetDN +
                "\n" + bundle.getString("getservicetemplate") +
                "\n" + serviceName);
        }

        AdminReq.writer.println(bundle.getString(getType()) + " " +
            targetDN + "\n" + bundle.getString("getservicetemplate") +
            "\n" + serviceName);

        AMTemplate tmpl = getTemplate(dpConnection);

        try {
            if ((tmpl == null) || !tmpl.isExists()) {
                throw new AdminException(
                    bundle.getString("templateDoesNotExist"));
            }

            PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
            prnUtl.printAVPairs(tmpl.getAttributes(), 2);
        } catch (AMException dpe) {
            throw new AdminException(dpe);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected String getType() {
        return "organization";
    }

    protected AMTemplate getTemplate(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            return org.getTemplate(serviceName, schemaType);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
