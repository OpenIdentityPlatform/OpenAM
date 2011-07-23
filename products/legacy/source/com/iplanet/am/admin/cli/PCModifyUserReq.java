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
 * $Id: PCModifyUserReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class PCModifyUserReq extends AdminReq {

    private Map userReq = new HashMap();
    private String userDN;

    /**
     * Constructs a new PCModifyUserReq.
     *
     * @param targetDN the PeopleContainer DN. 
     */        
    PCModifyUserReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the User DN and its avPair Map to the UserReq Map.
     *
     * @param modifyDN the User DN
     * @param avPair Map of attribute name to its values.
     */        
    void addUserReq(String modifyDN, Map avPair) {
        userDN = modifyDN;
        userReq = avPair;
    }

    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(bundle.getString("requestdescription70") +
            " " + targetDN + "\n" + "  " + userDN);
        prnUtl.printAVPairs(userReq, 2);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("peoplecontainer") + " " +
                targetDN + "\n" + bundle.getString("modifyuser"));
        }

        writer.println(bundle.getString("peoplecontainer") + " " + targetDN +
            "\n" + bundle.getString("modifyuser") + " " + userDN);

        try {
            AMUser user = dpConnection.getUser(userDN);
            doLog(user, AdminUtils.MODIFY_USER_ATTEMPT);
            user.setAttributes(userReq);
            user.store();
//            doLog(user, "modify-user");
            doLog(user, AdminUtils.MODIFY_USER);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
