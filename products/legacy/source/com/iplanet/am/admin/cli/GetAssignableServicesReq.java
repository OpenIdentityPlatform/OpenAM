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
 * $Id: GetAssignableServicesReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

class GetAssignableServicesReq extends AdminReq {
    private boolean recursiveSearch = false;
    private String realmPath = null;
    private String pattern = "*";        // default

    /**
     * Constructs a new GetAssignableServicesReq.
     *
     * @param  targetDN the Realm to delete. 
     */        
    GetAssignableServicesReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription103") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("getAssignableSvcs") + " " + targetDN +
            ", " + bundle.getString("orgServices"));

        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            Set gaSet = ocm.getAssignableServices();

            if (gaSet.size() == 0) {
                AdminReq.writer.println(" " + bundle.getString("none"));
            } else {
                for (Iterator it = gaSet.iterator(); it.hasNext(); ) {
                    String tmpS = it.next().toString();

                    ServiceSchemaManager mgr =
                        new ServiceSchemaManager(tmpS, ssoToken);
                    String i18nKey = null;
                    Set types = mgr.getSchemaTypes();
                    if (!types.isEmpty()) {
                        SchemaType type = (SchemaType)types.iterator().next();
                        ServiceSchema schema = mgr.getSchema(type);
                        if (schema != null) {
                            i18nKey = schema.getI18NKey();
                        }
                    }
                    //
                    //  only print the service name if it has
                    //  an i18nKey value (thus displayable)
                    //
                    if ((i18nKey != null) && (i18nKey.length() > 0)) {
                        AdminReq.writer.println(" " + tmpS);
                    }
                }
            }

            //
            //  now get the Realm Identity type
            //

            AdminReq.writer.println(bundle.getString("getAssignableSvcs") +
                " " + targetDN +
                ", " + bundle.getString("dynamicServices"));

            AMIdentityRepository amir = 
                new AMIdentityRepository (ssoToken, realmPath);
        
            AMIdentity ai = amir.getRealmIdentity();

            gaSet = ai.getAssignableServices();

            if (gaSet.size() == 0) {
                AdminReq.writer.println(" " + bundle.getString("none"));
                return;
            }

            for (Iterator it = gaSet.iterator(); it.hasNext(); ) {
                String tmpS = it.next().toString();

                ServiceSchemaManager mgr =
                    new ServiceSchemaManager(tmpS, ssoToken);
                String i18nKey = null;
                Set types = mgr.getSchemaTypes();
                if (!types.isEmpty()) {
                    SchemaType type = (SchemaType)types.iterator().next();
                    ServiceSchema schema = mgr.getSchema(type);
                    if (schema != null) {
                        i18nKey = schema.getI18NKey();
                    }
                }
                //
                //  only print the service name if it has
                //  an i18nKey value (thus displayable)
                //
                if ((i18nKey != null) && (i18nKey.length() > 0)) {
                    AdminReq.writer.println(" " + tmpS);
                }
            }
            
        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (IdRepoException idre) {
            throw new AdminException(idre);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
