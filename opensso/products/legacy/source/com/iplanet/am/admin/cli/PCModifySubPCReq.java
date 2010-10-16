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
 * $Id: PCModifySubPCReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
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
import java.util.Map;

class PCModifySubPCReq extends AdminReq {

    private Map subPCReq = new HashMap();
    private String pcDN;        

    /**
     * Constructs a new PCModifySubPCReq.
     *
     * @param  targetDN the PeopleContainer DN. 
     */        
    PCModifySubPCReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the PeopleContainert DN and its avPair Map to the SubPCReq Map.
     *
     * @param modifyDN the PeopleContainer DN
     * @param avPair the Map which contains the attribute as key and value
     */        
    void addSubPCReq(String modifyDN, Map avPair) {
        pcDN = modifyDN;
        subPCReq = avPair;
    }

    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(bundle.getString("requestdescription71") +
            " " + targetDN + "\n" + "  " + pcDN);
        prnUtl.printAVPairs(subPCReq, 2);
        prnWriter.flush();
        return stringWriter.toString();
    }

        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("peoplecontainer") + " " +
                targetDN + "\n" + bundle.getString("modifypeoplecontainer"));
        }

        writer.println(bundle.getString("peoplecontainer") + " " + targetDN +
            "\n" + bundle.getString("modifypeoplecontainer") + " " + pcDN);

        String[] args = {pcDN};
        try {
            AMPeopleContainer pc = dpConnection.getPeopleContainer(pcDN);
            doLog(pc, AdminUtils.MODIFY_PC_ATTEMPT);
            pc.setAttributes(subPCReq);
            pc.store();
//            doLog(pc, "modify-pc");
            doLog(pc, AdminUtils.MODIFY_PC);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
