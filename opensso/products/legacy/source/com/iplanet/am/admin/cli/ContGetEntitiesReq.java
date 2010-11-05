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
 * $Id: ContGetEntitiesReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMEntityType;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class ContGetEntitiesReq
    extends SearchReq
{
    private Set entityDNs = new HashSet();
    private boolean DNsOnly = true;
    private String entityType = null;

    /**
     * Constructs a new handle to get entity profile.
     *
     * @param targetDN container distinguished name.
     */
    ContGetEntitiesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Sets the value for DNsOnly which tells the process() method to get only
     * the DNs or all the information.
     *
     * @param DNsOnly  The new dNsOnly value
     */
    void setDNsOnly(boolean DNsOnly) {
        this.DNsOnly = DNsOnly;
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
     * Adds the dn to Set entityDNs which holds all the entity
     * dn's.
     *
     * @param dns entities dns.
     */
    void addDNs(Set dns) {
        if (dns != null) {
            entityDNs = dns;
        }
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription91") +
            " " + targetDN);
        prnWriter.println("   DNsOnly = " + DNsOnly);
        prnWriter.println("   filter = " + filter);
        prnWriter.println("   sizeLimit = " + sizeLimit);
        prnWriter.println("   timeLimit = " + timeLimit);
        
        if (entityDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(entityDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * This method prints all the Entity information for an container based on
     * the values if the Entity DNs set is empty than it prints all the entities.
     * if DNsOnly is true than it prints only the DNs of the entities else it
     * prints all the information of the all the entities.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        EntityUtils entityUtils = new EntityUtils();
        Map map = entityUtils.getEntityTypesMap(dpConnection);
        AMEntityType type  = (AMEntityType)map.get(entityType);
        if (type == null) {
             throw new AdminException(bundle.getString("invalidEntity"));
        }
        String args[] =  { entityUtils.getL10NAttributeName(dpConnection, 
                   type.getServiceName(), ENTITIES_DESC) };
        String localizedStr = MessageFormat.format(
            bundle.getString("getEntities"), args);
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("container") + " " + targetDN + "\n" +
            localizedStr);

        try {
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);
            AdminReq.writer.println(targetDN);
            boolean needValidation = false;

            if (entityDNs.isEmpty()) {
                AMSearchControl searchCtrl = createSearchControl(
                    AMConstants.SCOPE_ONE);
                AMSearchResults searchResults = orgUnit.searchEntities(
                    filter, null, type.getSearchTemplate(), searchCtrl);
                errorCode = searchResults.getErrorCode();
                entityDNs = searchResults.getSearchResults();
            } else {
                needValidation = true;
            }

            for (Iterator iter = entityDNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                AMEntity entity = dpConnection.getEntity(dn);
                                                                                
                if (!needValidation ||
                    (entity.isExists() &&
                        AdminUtils.isDescendantOf(entity, targetDN,
                            AMConstants.SCOPE_ONE))
                ) {
                    EntityUtils.printEntityInformation(prnUtl, entity,
                        dpConnection, DNsOnly);
                }
            }
            printSearchLimitError();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
