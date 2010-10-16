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
 * $Id: OrgModifyPCSTemplateReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.*;
import com.iplanet.sso.*;

import java.util.*;

class OrgModifyPCSTemplateReq extends AdminReq {

    // contains ServiceTemplateDN as attribute and avPair as value        
    private Map PCSTemplateReq=null;
    private String PCSTemplateDN=null;

    /**
     * Constructs a new empty OrgModifyPCSTemplateReq.
     */
    OrgModifyPCSTemplateReq() {
        super();
        PCSTemplateReq = new HashMap();
    }

    /**
     * Constructs a new OrgModifyServiceTemplateReq.
     * @param  targetDN the Organization DN. 
     */        
    OrgModifyPCSTemplateReq(String targetDN) {
        super(targetDN);
        PCSTemplateReq = new HashMap();
    }

    /**
     * Adds the role dn's and its avPair Map to ServiceTemplateReq Map.
     *
     * @param RoleDN the DN of a group
     * @param avPair Map of attribute name to values.
     */        
    void addPCSTemplateReq(String modifyDN, Map avPair) {
             PCSTemplateReq = avPair;
             PCSTemplateDN = modifyDN;
    }
        
    /**
     * Returns map of sub organization DN to Map of attribute name to values.
     *
     * @return map of sub organization DN to Map of attribute name to values. 
     */
    Map getPCSTemplateReq() {
        return PCSTemplateReq;
    }
    

    void process(AMStoreConnection dpConnection)
        throws AdminException {
        try {
            if (AdminUtils.logEnabled())
                AdminUtils.log("PCServiceTmplDN: " + PCSTemplateDN);
            AMOrganization org = dpConnection.getOrganization(PCSTemplateDN);
            org.setAttributes(PCSTemplateReq);
            org.store();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        } catch (Exception e) {
            throw new AdminException(e.toString());
        }


    }
        
                   
                                  
}
