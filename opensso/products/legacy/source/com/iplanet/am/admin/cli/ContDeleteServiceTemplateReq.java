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
 * $Id: ContDeleteServiceTemplateReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class ContDeleteServiceTemplateReq extends AdminReq {
    private String serviceName;
    private String schemaType;

    /**
     * Constructs a new empty ContDeleteServiceTemplateReq.
     */
    ContDeleteServiceTemplateReq() {
        super();
    }

    /**
     * Constructs a new ContDeleteServiceTemplateReq.
     *
     * @param targetDN the Organizational unit DN. 
     */        
    ContDeleteServiceTemplateReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription62") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }


    /**
     * Set the service name of template to be deleted.
     *
     * @param service name.
     * @param type of schema.
     */        
    void setServiceTemplateReq(String service, String type) {
        serviceName = service;
        schemaType = type;
    }
        
    void process(AMStoreConnection dpConnection, SSOToken ssoToken)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("deleteservicetemplates"));
        }
                                                                                
        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("deleteservicetemplates"));

        try {
            AMOrganizationalUnit org = dpConnection.getOrganizationalUnit(
                targetDN);
            List toDeleteSchemaTypes = getSchemaTypesToDelete();
            ServiceSchemaManager ssm = getServiceSchemaManager(
                ssoToken, serviceName);
            Set schemaTypes = ssm.getSchemaTypes();
            AdminReq.writer.println(serviceName);

            for (Iterator iter = schemaTypes.iterator(); iter.hasNext(); ) {
                SchemaType type = (SchemaType)iter.next();

                if (toDeleteSchemaTypes.contains(type)) {
                    doLog(serviceName, org,
                        AdminUtils.DELETE_SERVTEMPLATE_ATTEMPT);

                    if (type.equals(SchemaType.ORGANIZATION)) {
                        AMTemplate tmpl = org.getTemplate(serviceName,
                            AMTemplate.ORGANIZATION_TEMPLATE);
                        tmpl.delete();
//                        doLog(serviceName, org, "delete-servtemplate");
                        doLog(serviceName, org,
                            AdminUtils.DELETE_SERVTEMPLATE);
                    } else if (type.equals(SchemaType.DYNAMIC)) {
                        AMTemplate tmpl = org.getTemplate(serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                        tmpl.delete();
//                        doLog(serviceName, org, "delete-servtemplate");
                        doLog(serviceName, org,
                            AdminUtils.DELETE_SERVTEMPLATE);
                    } else if (type.equals(SchemaType.POLICY)) {
                        AMTemplate tmpl = org.getTemplate(serviceName,
                            AMTemplate.POLICY_TEMPLATE);
                        tmpl.delete();
//                        doLog(serviceName, org, "delete-servtemplate");
                        doLog(serviceName, org,
                            AdminUtils.DELETE_SERVTEMPLATE);
                    }
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private List getSchemaTypesToDelete() {
        List list = Collections.EMPTY_LIST;

        if ((schemaType == null) || (schemaType.length() == 0)) {
            list = new ArrayList(3);
            list.add(SchemaType.ORGANIZATION);
            list.add(SchemaType.DYNAMIC);
            list.add(SchemaType.POLICY);
        } else {
            if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_ORGANIZATION)) {
                list = new ArrayList(1);
                list.add(SchemaType.ORGANIZATION);
            } else if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_DYNAMIC)) {
                list = new ArrayList(1);
                list.add(SchemaType.DYNAMIC);
            } else if (schemaType.equalsIgnoreCase(SCHEMA_TYPE_POLICY)) {
                list = new ArrayList(1);
                list.add(SchemaType.POLICY);
            }
        }

        return list;
    }
}
