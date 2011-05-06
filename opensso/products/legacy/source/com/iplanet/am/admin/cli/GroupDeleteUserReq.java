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
 * $Id: GroupDeleteUserReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class GroupDeleteUserReq extends AddDeleteReq {
    /**
     * Constructs an unsubscribe user from group request handler.
     *
     * @param  targetDN the Group DN. 
     */        
    GroupDeleteUserReq(String targetDN) {
        super(targetDN);
    }
    
    /**
     * converts this object into a string.
     *
     * @return String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription80") +
            " "  + targetDN);

        if (DNSet.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(DNSet, 1);
        }

        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("group") + " " +
                targetDN + "\n" + bundle.getString("removeusers"));
        }
                                                                                
        writer.println(bundle.getString("group") + " " +
            targetDN + "\n" + bundle.getString("removeusers"));

        AMObject amo = null;
        try {
            int groupType = dpConnection.getAMObjectType(targetDN);
            if(groupType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
               AMAssignableDynamicGroup group = 
                          dpConnection.getAssignableDynamicGroup(targetDN);
               amo = group;
               doLogStringSet(DNSet, group,
                    AdminUtils.REMOVE_USER_FROM_GROUP_ATTEMPT);
               group.removeUsers(DNSet);
               doLogStringSet(DNSet, group, AdminUtils.REMOVE_USER_FROM_GROUP);
            } else if ((groupType == AMObject.STATIC_GROUP) ||
                (groupType == AMObject.GROUP)
            ) {
               AMStaticGroup group = dpConnection.getStaticGroup(targetDN);
               amo = group;
               doLogStringSet(DNSet, group,
                    AdminUtils.REMOVE_USER_FROM_GROUP_ATTEMPT);
               group.removeUsers(DNSet);
               doLogStringSet(DNSet, group, AdminUtils.REMOVE_USER_FROM_GROUP);
            } else {
               throw new AdminException(
                   bundle.getString("cannotRemoveUsersFromDynamicGroup")); 
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("group") + " " +
                    targetDN + "\n" + bundle.getString("removedusers"));
            }

        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
