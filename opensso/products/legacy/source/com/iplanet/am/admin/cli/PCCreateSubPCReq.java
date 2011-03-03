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
 * $Id: PCCreateSubPCReq.java,v 1.2 2008/06/25 05:52:31 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class PCCreateSubPCReq extends AdminReq {

    private Map subPCReq = new HashMap();

    /**
     * Constructs a new PCCreateSubPCReq.
     *
     * @param  targetDN the PeopleContainer DN. 
     */        
    PCCreateSubPCReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the PeopleContainer DN and its avPair Map to the subPCReq Map.
     *
     * @param pcDN the PeopleContainer DN
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addSubPCReq(String pcDN, Map avPair) {
        subPCReq.put(pcDN, avPair);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription46") +
            " " + targetDN);     
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, subPCReq);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("peoplecontainer") + " " +
                targetDN + "\n" + bundle.getString("addpeoplecontainers"));
        }

        writer.println(bundle.getString("peoplecontainer") + " " + targetDN +
            "\n" + bundle.getString("addpeoplecontainers"));

        Set pcNames = subPCReq.keySet();
        for (Iterator i = pcNames.iterator(); i.hasNext(); ) {
            String pcName = (String)i.next();
            String[] params = {pcName, targetDN};
            doLog(params, AdminUtils.CREATE_PC_ATTEMPT);
        }

        try {
            AMPeopleContainer pcr = dpConnection.getPeopleContainer(targetDN);
            Set set = pcr.createSubPeopleContainers(subPCReq); 

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg25"));
            }

            doLog(set, AdminUtils.CREATE_PC);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
