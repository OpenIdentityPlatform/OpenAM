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
 * $Id: ContCreateServiceTemplateReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class ContCreateServiceTemplateReq extends AdminReq {

    private Set serviceNames = new HashSet();
                    
    /**
     * Constructs a new empty ContCreateServiceTemplateReq.
     */
    ContCreateServiceTemplateReq() {
        super();      
    }

    /**
     * Constructs a new ContCreateServiceTemplateReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    ContCreateServiceTemplateReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription63") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Add service name of template to be created.
     *
     * @param name of service
     */
    void addContServiceTmplReq(String name) {
        serviceNames.add(name);
    }

    void process(AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("createservicetemplates"));
        }

        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("createservicetemplates"));

        try {
            AMOrganizationalUnit orgUnit =
                connection.getOrganizationalUnit(targetDN);

            for (Iterator iter = serviceNames.iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();
                createServiceTemplate(orgUnit, serviceName, ssoToken);
                AdminReq.writer.println(serviceName);
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private void createServiceTemplate(AMOrganizationalUnit orgUnit,
        String serviceName, SSOToken ssoToken)
        throws AdminException
    {
        try {
            ServiceSchemaManager ssm = getServiceSchemaManager(ssoToken,
                serviceName);
            Set schemaTypes = ssm.getSchemaTypes();

            for (Iterator iter = schemaTypes.iterator(); iter.hasNext(); ) {
                SchemaType type = (SchemaType)iter.next();

                if (type.equals(SchemaType.ORGANIZATION)) {

                    doLog(serviceName, orgUnit,
                        AdminUtils.CREATE_SERVTEMPLATE_ATTEMPT);

                    AMTemplate template = orgUnit.createTemplate(
                        AMTemplate.ORGANIZATION_TEMPLATE, serviceName, null);

                    if (serviceName.equals(AUTH_SERVICE)) {
                        String pcDN = AdminUtils.getPeopleContainerDN(orgUnit);

                        if (pcDN != null) {
                            Set set = new HashSet();
                            set.add(pcDN);
                            Map map = new HashMap();
                            map.put(AUTH_USER_CONTAINER_ATTRIBUTE, set);
                            template.setAttributes(map);
                            template.store();
                        }
                    }

//                    doLog(serviceName, orgUnit, "create-servtemplate");
                    doLog(serviceName, orgUnit,
                        AdminUtils.CREATE_SERVTEMPLATE);
                } else if (type.equals(SchemaType.DYNAMIC)) {
                    doLog(serviceName, orgUnit,
                        AdminUtils.CREATE_SERVTEMPLATE_ATTEMPT);

                    orgUnit.createTemplate(AMTemplate.DYNAMIC_TEMPLATE,
                        serviceName, null, 0);
//                    doLog(serviceName, orgUnit, "create-servtemplate");
                    doLog(serviceName, orgUnit,
                        AdminUtils.CREATE_SERVTEMPLATE);
                }
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
