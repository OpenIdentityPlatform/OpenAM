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
 * $Id: OrgCreateUserReq.java,v 1.2 2008/06/25 05:52:29 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.sm.SchemaType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class OrgCreateUserReq extends AdminReq {

    private Map userReq = new HashMap();
    private SSOToken ssoToken;

    /**
     * Constructs a new <code>OrgCreateUserReq</code>.
     *
     * @param targetDN the PeopleContainer DN. 
     * @param ssoToken Single-Sign-On token.
     */        
    OrgCreateUserReq(String targetDN, SSOToken ssoToken) {
        super(targetDN);
        this.ssoToken = ssoToken;
    }

    /**
     * adds the User DN and its avPair Map to the UserReq Map.
     *
     * @param userDN the User DN
     * @param avPair the Map which contains the attribute as key and value as
     *        value        
     */        
    void addUserReq(String userDN, Map avPair) {
        userReq.put(userDN, avPair);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription82") +
            " "  + targetDN); 
        AdminUtils.printAttributeNameValuesMap(
            prnWriter, prnUtl, ssoToken, userReq, AdminUtils.USER_SERVICE,
                SchemaType.USER);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        writer.println(bundle.getString("organization") + " " + targetDN +
            "\n" + bundle.getString("createusers"));

        try {
            Set userDNs = userReq.keySet();
            for (Iterator i = userDNs.iterator(); i.hasNext(); ) {  
                String userDN = (String)i.next();
                String[] args = {userDN, targetDN};
                doLog(args, AdminUtils.CREATE_USER_ATTEMPT);
            }

            AMOrganization org = dpConnection.getOrganization(targetDN);
            AMPeopleContainer pc = getDefaultPeopleContainer(dpConnection, org);
            Set users = pc.createUsers(userReq);

            if (AdminUtils.logEnabled()) {
               AdminUtils.log(AdminReq.bundle.getString("statusmsg42"));
            }

            for (Iterator iter = users.iterator(); iter.hasNext(); ) {
                AMUser user = (AMUser)iter.next();
                writer.println(user.getDN());
            }

//            doLog(users, "create-user");
            doLog(users, AdminUtils.CREATE_USER);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
    
    
    private AMPeopleContainer getDefaultPeopleContainer(
        AMStoreConnection connection,
        AMOrganization org)
        throws AdminException
    {
        AMPeopleContainer peopleContainer = null;

        try {
            String dn = AdminInterfaceUtils.getNamingAttribute(
                    AMObject.PEOPLE_CONTAINER, debug) +
                "=" + AdminInterfaceUtils.defaultPeopleContainerName() + "," +
                org.getDN();
            peopleContainer = connection.getPeopleContainer(dn);

            if ((peopleContainer == null) || !peopleContainer.isExists()) {
                String[] strArray = {dn};
                String errorMesage = MessageFormat.format(
                    bundle.getString(
                        "defaultPeopleContainerNotFound"), strArray);
                throw new AdminException(errorMesage);
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return peopleContainer;
    }
}

