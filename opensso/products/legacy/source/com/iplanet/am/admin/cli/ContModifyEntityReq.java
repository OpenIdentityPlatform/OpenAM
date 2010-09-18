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
 * $Id: ContModifyEntityReq.java,v 1.2 2008/06/25 05:52:26 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMEntityType;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

class ContModifyEntityReq
    extends AdminReq
{
    private Map values = new HashMap();
    private String entityDN;
    private String entityType = null;
                
    /**
     * Constructs a new handle for modifying entity in a container.
     *
     * @param targetDN container DN. 
     */        
    ContModifyEntityReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Adds entity distinguished name and its values.
     *
     * @param dn distinguished name of entity.
     * @param values Map of attribute name to values.        
     */        
    void addRequest(String dn, Map values) {
        entityDN = dn;
        this.values = values;
    }

    /**
     * Sets the entity type.
     *
     * @param entityType entity type.
     */
    void setEntityType(String entityType) {
        this.entityType = entityType;
    }
        
    /**
     * Returns a string equivalent of this request handle.
     *
     * @return a string equivalent of this request handle. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription86") +
            " "  + targetDN + "\n" + "  " + entityDN); 
        prnUtl.printAVPairs(values, 2);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        EntityUtils entityUtils = new EntityUtils();
        Map map = entityUtils.getEntityTypesMap(dpConnection);
        AMEntityType type  = (AMEntityType)map.get(entityType);
        if (type == null) {
             throw new AdminException(bundle.getString("invalidEntity"));
        }
        String localizedName = entityUtils.getL10NAttributeName(
            dpConnection, type.getServiceName(), ENTITY_NAME);
        String args[] = { localizedName };

        String localizedStr = MessageFormat.format(
            bundle.getString("modifyEntity"), args);

        writer.println(
            bundle.getString("container") + " " + targetDN + "\n" +
                localizedStr);
        
        try {
            AMEntity entity = dpConnection.getEntity(entityDN);
            doLog(entity, AdminUtils.MODIFY_ENTITY_ATTEMPT, localizedName);
            entity.setAttributes(values);
            entity.store();
//            doLog(entity, "modify-entity", localizedName);
            doLog(entity, AdminUtils.MODIFY_ENTITY, localizedName);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
