/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMDynamicGroupImpl.java,v 1.5 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * The <code>AMDynamicGroupImpl</code> implements interface 
 * <code>AMDynamicGroup</code> dynamic group.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMDynamicGroupImpl extends AMGroupImpl implements AMDynamicGroup {

    public AMDynamicGroupImpl(SSOToken ssoToken, String dn) {
        super(ssoToken, dn, DYNAMIC_GROUP);
    }

    /**
     * Returns the filter for the dynamic group.
     *
     * @return the filter for the dynamic group.
     * @throws AMException if an error is encountered when trying to 
     *         access/retrieve data from the data store.
     * @throws SSOException if the single sign on token is no longer valid.
     */
    public String getFilter() throws AMException, SSOException {
        String[] array = dsServices.getGroupFilterAndScope(token, entryDN,
                profileType);
        return (array[2]);
    }

    /**
     * Sets the the filter for the dynamic group.
     *
     * @param filter the dynamic group filter.
     * @throws AMException if an error is encountered when trying to 
     *         access/retrieve data from the data store.
     * @throws SSOException if the single sign on token is no longer valid.
     */
    public void setFilter(String filter) throws AMException, SSOException {
        dsServices.setGroupFilter(token, entryDN, filter);
        setACI();
    }

    /**
     * Sets the aci and the role aci value for the dynamic group.
     *
     */
    private void setACI() {
        try {
            DN thisDN = new DN(entryDN);
            String orgDN = this.getOrganizationDN();
            String roleDN = AMNamingAttrManager.getNamingAttr(AMObject.ROLE)
                    + "=" + thisDN.toString().replace(',', '_') + "," + orgDN;
            AMStoreConnection amsc = new AMStoreConnection(token);
            AMRole gRole = amsc.getRole(roleDN);

            Set aciValue = null;
            aciValue = gRole.getAttribute("iplanet-am-role-aci-list");
            Iterator iter = aciValue.iterator();
            Set newACIValue = new HashSet();

            while (iter.hasNext()) {
                String aci = (String) iter.next();
                int indx = aci.indexOf("iplanet-am-static-group-dn=");
                if (indx < 0) {
                    newACIValue.add(aci);
                } else {
                    String targetFilter = aci.substring(0, indx);
                    String restACI = aci.substring(aci.indexOf("(|(nsroledn"));
                    StringBuilder sb = new StringBuilder();
                    sb.append(targetFilter).append(
                            "iplanet-am-static-group-dn=*").append(entryDN)
                            .append(")").append(this.getFilter()).append("))")
                            .append(restACI);
                    newACIValue.add(sb.toString());
                }
            }
            HashMap avPairs = new HashMap(1);
            avPairs.put("iplanet-am-role-aci-list", newACIValue);
            try {
                gRole.setAttributes(avPairs);
                gRole.store();
            } catch (AMException ame) {
                if (debug.warningEnabled()) {
                    debug.warning("error setting attribute ", ame);
                }
            } catch (SSOException soe) {
                debug.error("Error in SSO Token");
            }
        } catch (AMException amex) {
            if (debug.warningEnabled()) {
                debug.warning("Could not set aci " + amex);
            }
        } catch (SSOException soe) {
            debug.error("Error in SSO Token" + soe);
        }
    }
}
