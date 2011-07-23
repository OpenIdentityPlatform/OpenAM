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
 * $Id: OrgModifyServiceTemplateReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.Guid;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.cos.COSManager;
import com.iplanet.ums.cos.DirectCOSDefinition;
import com.iplanet.ums.cos.ICOSDefinition;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

class OrgModifyServiceTemplateReq extends AdminReq {
    private Map ServiceTemplateReq = new HashMap();
    private String serviceName;
    private int stype;
    private String scope = "SCOPE_ONE";
    private boolean roleTemplate = false;
    private SSOToken ssoToken = null;
    
    /**
     * Constructs a new empty OrgModifyServiceTemplateReq.
     */
    OrgModifyServiceTemplateReq() {
        super();
    }

    /**
     * Constructs a new OrgModifyServiceTemplateReq.
     *
     * @param targetDN the Organization DN. 
     */        
    OrgModifyServiceTemplateReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription65") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void addServiceTemplateReq(String service, String schemaType, 
         String level, String roleTemp, Map avPair, SSOToken ssoToken)
         throws AdminException
    {
        if ((service == null) || (service.length() == 0) ||
            (schemaType == null) || (schemaType.length() == 0)
        ) {
            throw new UnsupportedOperationException();
             }

        serviceName = service;
        ServiceTemplateReq = avPair;
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
        }
    }
        
    /**
     * gets the subOrgReq Map which contains key as subOrg DN and value as
     * Map of avPairs.
     *
     * @return ServiceTemplateReq. 
     */
    Map getServiceTemplateReq() {
        return ServiceTemplateReq;
    }
    
    void modifyServiceTemplates(AMStoreConnection dpConnection, String dn,
        ServiceSchema serviceSchema)
        throws AdminException
    {
        try {
            /*
             * This is a temperory fix for COSDefinition updates for 
             * new Attributes this code should be removed once SDK takes
             * care of this.
             */
            Guid guid = new Guid(dn);
            DirectCOSDefinition cosDefinition = null;
            COSManager cosManager = COSManager.getCOSManager(ssoToken, guid);

            try {
                cosDefinition =
                    (DirectCOSDefinition) cosManager.getDefinition(serviceName);
            } catch (Exception e) {
                /* The exception is ignored because COSManager.getDifinition
                 * will throw an exception if COSDefinition does not exist
                 */
            }

            if (cosDefinition != null) {
                String[] array = cosDefinition.getCOSAttributes();
                Set oldAttrs = new HashSet();

                for (int i = 0; i < array.length; i++) {
                    StringTokenizer st = new StringTokenizer(array[i]);
                    oldAttrs.add(st.nextToken());
                }

                Set attSet = ServiceTemplateReq.keySet();
                Iterator newAttrItr = attSet.iterator();

                while (newAttrItr.hasNext()) {
                    boolean valueExists = false;
                    String newAttr = newAttrItr.next().toString();
                    Iterator itr = oldAttrs.iterator();

                    while (itr.hasNext()) {
                        if (newAttr.equalsIgnoreCase(itr.next().toString())) {
                            valueExists = true;
                            break;
                        }
                    }

                    if (!valueExists) {
                        AttributeSchema attrSchema =
                            serviceSchema.getAttributeSchema(newAttr);
                        String name = attrSchema.getCosQualifier();
                        cosDefinition.addCOSAttribute(newAttr,
                            convertCosQualifier(name));
                        cosDefinition.save();
                    }
                }
            }

            AMOrganization org = dpConnection.getOrganization(dn);
            AMTemplate tmpl = org.getTemplate(serviceName, stype);

            if (tmpl.isExists()) {
                doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE_ATTEMPT);
                tmpl.setAttributes(ServiceTemplateReq);
                tmpl.store();
//              doLog(tmpl, "modify-servtemplate");
                doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE);
            } else {
                throw new AdminException
                        (bundle.getString("templatedoesnotexist"));
            }

            if (roleTemplate) {
                Set roles = org.getRoles(AMConstants.SCOPE_ONE);

                for (Iterator iter = roles.iterator(); iter.hasNext();) {
                    String roleDN = (String) iter.next();
                    AMRole role = dpConnection.getRole(roleDN);
                    tmpl = role.getTemplate(serviceName, stype);

                    if (tmpl.isExists()) {
                        doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE_ATTEMPT);
                        tmpl.setAttributes(ServiceTemplateReq);
                        tmpl.store();
//                      doLog(tmpl, "modify-servtemplate");
                        doLog(tmpl, AdminUtils.MODIFY_SERVTEMPLATE);
                    } else {
                        throw new AdminException
                           (bundle.getString("roletemplatedoesnotexist"));
                    }
                }
            }
        } catch (SSOException e) {
            throw new AdminException(e);
        } catch (AMException e) {
            throw new AdminException(e);
        } catch (UMSException e) {
            throw new AdminException(e);
        }
    }

    private int convertCosQualifier(String qual) {
        int definition = ICOSDefinition.DEFAULT;

        if (qual.equalsIgnoreCase("default")) {
            definition = ICOSDefinition.DEFAULT;
        } else if (qual.equalsIgnoreCase("override")) {
            definition = ICOSDefinition.OVERRIDE;
        } else if (qual.equalsIgnoreCase("merge-schemes")) {
            definition = ICOSDefinition.MERGE_SCHEMES;
        } else if (qual.equalsIgnoreCase("operational")) {
            definition = ICOSDefinition.OPERATIONAL;
        }
        
        return definition;
    }

    void process(AMStoreConnection dpConnection) throws AdminException {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("organization") + " " + targetDN +
                "\n" + bundle.getString("modifyservicetemplates") + "\n" +
                serviceName);
        }

        AdminReq.writer.println(bundle.getString("organization") + " " +
            targetDN + "\n" + bundle.getString("modifyservicetemplates") +
            "\n" + serviceName);

        try {
            ServiceSchemaManager ssm = null;
            ServiceSchema ss = null;

            if (stype != -1) {
                if (stype == AMTemplate.DYNAMIC_TEMPLATE) {
                    ssm = new ServiceSchemaManager(serviceName, ssoToken);
                    try {
                        ss = ssm.getSchema(SchemaType.DYNAMIC);
                    } catch (SMSException sme) {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "AMSDKUtil.getServiceNames(): No schema "
                                    + "defined for SchemaType.DYNAMIC type");
                        }
                    }
                }

                modifyServiceTemplates(dpConnection, targetDN, ss);

                if (scope.equalsIgnoreCase("SCOPE_SUB")) {
                    AMOrganization org = dpConnection.getOrganization(targetDN);
                    Set orgset = org.getSubOrganizations(AMConstants.SCOPE_SUB);

                    for (Iterator iter = orgset.iterator(); iter.hasNext();) {
                        modifyServiceTemplates( dpConnection,
                            (String)iter.next(), ss);
                    }
                }
            } else {
                throw new AdminException(bundle.getString("invalidArguments"));
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}
