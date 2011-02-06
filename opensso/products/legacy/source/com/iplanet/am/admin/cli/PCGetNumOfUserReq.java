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
 * $Id: PCGetNumOfUserReq.java,v 1.2 2008/06/25 05:52:31 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class PCGetNumOfUserReq extends AdminReq {
    /**
     * Constructs a new PCGetNumOfUserReq.
     * @param targetDN the PeopleContainer DN. 
     */        
    PCGetNumOfUserReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription50") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }   

    /**
     * This method prints the Number of Users for a PeopleContainer.
     *
     * @param dpConnection the AMStoreConnection.
     * throws AdminException if the syntax of the DN is not correct.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("peoplecontainer") + " " +
                targetDN + "\n" + bundle.getString("numberofusers"));
        }

        writer.println(bundle.getString("peoplecontainer") + " " + targetDN);

        try {
            AMPeopleContainer pc = dpConnection.getPeopleContainer(targetDN);
            long numOfUsers = pc.getNumberOfUsers();
            writer.println(bundle.getString("numberofusers") + " " +
                numOfUsers);
            AdminReq.writer.flush();
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
