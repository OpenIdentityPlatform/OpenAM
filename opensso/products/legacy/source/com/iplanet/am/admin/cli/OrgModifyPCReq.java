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
 * $Id: OrgModifyPCReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.Map;

class OrgModifyPCReq extends AdminReq {
    private Map pcReq;
    private String pcDN;
        
    /**
     * Constructs a new OrgModifyPcReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgModifyPCReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Modifies the peoplecontainer DN and its avPair Map to the PCReq Map.
     *
     * @param modifyDN the PeopleContainer DN
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addPCReq(String modifyDN, Map avPair) {
        pcReq = avPair;
        pcDN = modifyDN;
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("organization") + " " + targetDN +
                "\n" + bundle.getString("modifypeoplecontainer") + " " + pcDN);
        }
                                                                                
        writer.println(bundle.getString("organization") + " " + targetDN +
            "\n" + bundle.getString("modifypeoplecontainer") + " " + pcDN);

        try {
            AMPeopleContainer dppc = dpConnection.getPeopleContainer(pcDN);
            doLog(dppc, AdminUtils.MODIFY_PC_ATTEMPT);
            dppc.setAttributes(pcReq);
            dppc.store();

//            doLog(dppc, "modify-pc");
            doLog(dppc, AdminUtils.MODIFY_PC);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
