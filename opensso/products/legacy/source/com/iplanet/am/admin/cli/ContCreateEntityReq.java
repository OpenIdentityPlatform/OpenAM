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
 * $Id: ContCreateEntityReq.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMEntityType;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.Constants;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class ContCreateEntityReq
    extends AdminReq
{
    private Map entityReq = new HashMap();
    private String entityType = null;
    private SSOToken ssoToken;
                
    /**
     * Constructs a new handle for creating entity in a container.
     *
     * @param targetDN container DN. 
     * @param ssoToken User's single sign on token.
     */        
    ContCreateEntityReq(String targetDN, SSOToken ssoToken) {
        super(targetDN);
        this.ssoToken = ssoToken;
    }

    /**
     * Adds entity name and its values.
     *
     * @param name name of entity.
     * @param values Map of attribute name to values.        
     */        
    void addEntity(String name, Map values) {
        entityReq.put(name, values);
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
    public String toString(AMStoreConnection dpConnection) {
        EntityUtils entityUtils = new EntityUtils();
        Map map = entityUtils.getEntityTypesMap(dpConnection);
        AMEntityType type  = (AMEntityType)map.get(entityType);
        if (type == null) {
            return "";
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription84") +
            " "  + targetDN); 
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, ssoToken,
            entityReq, type.getServiceName(), null);
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
        String localizedName = entityUtils.getL10NAttributeName(dpConnection, 
            type.getServiceName(), ENTITY_NAME);
        String args[] =  { localizedName };
        String localizedStr = MessageFormat.format(
            bundle.getString("createEntity"), args);
        writer.println(bundle.getString("container") + " "
            + targetDN + "\n" + localizedStr);

        Set entityNames = entityReq.keySet();
        for (Iterator i = entityNames.iterator(); i.hasNext(); ) {
            String entityName = (String)i.next();
            String[] params = {localizedName, entityName, targetDN};
            doLog(params, AdminUtils.CREATE_ENTITY_ATTEMPT);
        }

        try {
            AMOrganizationalUnit ou =
                dpConnection.getOrganizationalUnit(targetDN);

            Set entities = ou.createEntities(type.getName(), entityReq);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg44"));
            }

            for (Iterator iter = entities.iterator(); iter.hasNext();) {
                AMEntity entity = (AMEntity)iter.next();
                writer.println(entity.getDN());
            }

            doLogForEntity(entities, AdminUtils.CREATE_ENTITY, localizedName);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
