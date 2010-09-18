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
 * $Id: ServiceTemplateAttrValuesReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


abstract class ServiceTemplateAttrValuesReq extends AdminReq {
    protected Map mapAttrValues = new HashMap();
    protected String serviceName;
    protected int stype;
    protected String scope = "SCOPE_ONE";
    protected boolean roleTemplate = false;
    protected SSOToken ssoToken = null;
    
    /**
     * Constructs a new instance.
     *
     * @param targetDN distinguished name of target. 
     */        
    ServiceTemplateAttrValuesReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString(getDescriptionString()) +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    abstract String getDescriptionString();

    void addRequest(
        String service,
        String schemaType,
        String level,
        String roleTemp,
        Map avPair,
        SSOToken ssoToken
    ) throws AdminException
    {
        if ((service == null) || (service.length() == 0) ||
            (schemaType == null) || (schemaType.length() == 0)
        ) {
            throw new UnsupportedOperationException();
             }

        serviceName = service;
        mapAttrValues = avPair;
        scope = level;
        roleTemplate = roleTemp.equalsIgnoreCase("true");
        stype = -1;
        this.ssoToken = ssoToken;

        if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_DYNAMIC)) {
            stype = AMTemplate.DYNAMIC_TEMPLATE;
        } else if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_ORGANIZATION)) {
            stype = AMTemplate.ORGANIZATION_TEMPLATE;
        } else if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_POLICY)) {
            stype = AMTemplate.POLICY_TEMPLATE;
        } else {
            throw new UnsupportedOperationException();
        }
    }
        
    abstract void modifyServiceTemplates(
        AMStoreConnection dpConnection,
        String dn
    ) throws AdminException;

    AMTemplate getTemplate(AMRole role)
        throws AdminException
    {
        AMTemplate tmpl = null;

        try {
            tmpl = role.getTemplate(serviceName, stype);

            if ((tmpl != null) && !tmpl.isExists()) {
                tmpl = null;
            }
        } catch (AMException ame) {
            // exception thrown is template does not exist.
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(ame.toString());
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return tmpl;
    }

    AMTemplate getTemplate(AMOrganization org)
        throws AdminException
    {
        AMTemplate tmpl = null;

        try {
            tmpl = org.getTemplate(serviceName, stype);

            if ((tmpl != null) && !tmpl.isExists()) {
                tmpl = null;
            }
        } catch (AMException ame) {
            // exception thrown is template does not exist.
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(ame.toString());
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return tmpl;
    }

    AMTemplate getTemplate(AMOrganizationalUnit ou)
        throws AdminException
    {
        AMTemplate tmpl = null;

        try {
            tmpl = ou.getTemplate(serviceName, stype);

            if ((tmpl != null) && !tmpl.isExists()) {
                tmpl = null;
            }
        } catch (AMException ame) {
            // exception thrown is template does not exist.
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(ame.toString());
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return tmpl;
    }

    abstract void process(AMStoreConnection dpConnection)
        throws AdminException;
}
