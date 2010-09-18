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
 * $Id: ContModifySubContReq.java,v 1.2 2008/06/25 05:52:26 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.Map;

class ContModifySubContReq extends AdminReq {
    private Map subContReq;
    private String subContDN;
                
    /**
     * Constructs a new ContModifySubContReq.
     *
     * @param  targetDN the Container DN. 
     */        
    ContModifySubContReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the subCont DN and its avPair Map to the subContReq Map.
     *
     * @param modifyDN the SubContainer DN
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addSubContReq(String modifyDN, Map avPair) {
        subContReq = avPair;
        subContDN = modifyDN;
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("modifycontainer") + " " + subContDN);
        }
                                                                                
        writer.println(bundle.getString("container") + " " + targetDN +
            "\n" + bundle.getString("modifycontainer") + " " + subContDN);

        try {
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(subContDN);
            doLog(orgUnit, AdminUtils.MODIFY_SUBCONT_ATTEMPT);
            orgUnit.setAttributes(subContReq);
            orgUnit.store();
//            doLog(orgUnit, "modify-subcont");
            doLog(orgUnit, AdminUtils.MODIFY_SUBCONT);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
