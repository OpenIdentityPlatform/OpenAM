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
 * $Id: ContDeleteEntitiesReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class ContDeleteEntitiesReq
    extends AddDeleteReq
{
    private String entityType = null;

    /**
     * Constructs a new handle to remove entities from container.
     *
     * @param targetDN container distinguished name.
     */        
    ContDeleteEntitiesReq(String targetDN) {
        super(targetDN);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription85") +
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
        EntityUtils entityUtils = new EntityUtils();
        Map map = entityUtils.getEntityTypesMap(dpConnection);
        AMEntityType type  = (AMEntityType)map.get(entityType);
        if (type == null) {
             throw new AdminException(bundle.getString("invalidEntity"));
        }
        String localizedName =   entityUtils.getL10NAttributeName(
            dpConnection, type.getServiceName(), ENTITY_NAME);
        String localizedDesc =   entityUtils.getL10NAttributeName(
            dpConnection, type.getServiceName(), ENTITIES_DESC);
        String args[] =  { localizedDesc };
        String localizedStr = MessageFormat.format(
             bundle.getString("deleteEntities"), args);

        writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + localizedStr);

        try {
            doLogStringSet(DNSet, AdminUtils.DELETE_ENTITY_ATTEMPT);
            Set deletedEntities = new HashSet();
            AMOrganizationalUnit ou =
                dpConnection.getOrganizationalUnit(targetDN);

            for (Iterator iter = DNSet.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();

                if (deleteEntity(dpConnection, ou, dn)) {
                    deletedEntities.add(dn);
                }
            }

            doLogStringSetForEntity(deletedEntities, AdminUtils.DELETE_ENTITY,
                localizedName);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private boolean deleteEntity(
        AMStoreConnection dpConnection,
        AMOrganizationalUnit ou,
        String dn
    ) {
        boolean deleted = false;
        AMEntity entity = null;

        try {
            entity = dpConnection.getEntity(dn);

            if (!entity.isExists()) {
                entity = null;
            }
        } catch (SSOException ssoe) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(ssoe.getMessage());
            }
        }

        if (entity != null) {
            try {
                entity.delete(true);
                deleted = true;
            } catch (AMException ame) {
                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(ame.getMessage());
                }
            } catch (SSOException ssoe) {
                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(ssoe.getMessage());
                }
            }
        } else {
            if (AdminUtils.logEnabled()) {
                String[] arr = {dn};
                String msg = bundle.getString("entityDoNotExists");
                AdminUtils.log(MessageFormat.format(msg, arr));
            }
        }

        return deleted;
    }
}
