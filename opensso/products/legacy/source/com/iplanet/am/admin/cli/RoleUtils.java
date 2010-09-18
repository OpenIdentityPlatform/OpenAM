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
 * $Id: RoleUtils.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;


import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The <code>AdminUtils</code> class provides common role helper methods.
 */
class RoleUtils {
    /**
     * Return role object based on role type.
     *
     * @param dn distinguished name of role.
     * @param connection Store connection object.
     * @param roleType type of role.
     * @return role object based on role type.
     */
    static AMRole getRole(String dn, AMStoreConnection connection, int roleType)
        throws AdminException
    {
        AMRole role = null;

        try {
            switch (roleType) {
            case AMObject.ROLE:
                role = connection.getRole(dn);
                break;
            case AMObject.FILTERED_ROLE:
                role = connection.getFilteredRole(dn);
                break;
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }

        return role;
    }

    /**
     * Returns role type.
     *
     * @param dn Distinguished name of role object.
     * @param connection Store connection object.
     * @return role type.
     * @throws AdminException if fails to get type of object.
     */
    static int getRoleType(String dn, AMStoreConnection connection)
        throws AdminException
    {
        try {
            return connection.getAMObjectType(dn);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    /**
     * Prints role information on line.
     *
     * @param prnUtl Print writer.
     * @param roleDNs Set of role distinguished names.
     * @param connection Store connection object.
     * @param bundle Resource bundle.
     * @param roleType type of role.
     */
    static void printRoleInformation(PrintUtils prnUtl, Set roleDNs,
        AMStoreConnection connection, ResourceBundle bundle, int roleType)
        throws AdminException
    {
        try {
            for (Iterator iter = roleDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMRole role = getRole(dn, connection, roleType);
                Map values = role.getAttributes();
                AdminReq.writer.println("  " + dn);
                prnUtl.printAVPairs(values, 2);
            }
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
