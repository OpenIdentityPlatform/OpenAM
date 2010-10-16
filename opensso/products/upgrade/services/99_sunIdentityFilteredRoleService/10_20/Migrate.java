/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:54:06 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;

/**
 * Updates the <code>sunIdentityFilteredRoleService</code> service schema.
 * This class is invoked during migration from older versions of Access Manager
 * to the latest version.
 */
public class Migrate implements MigrateTasks {

    final String serviceName = "sunIdentityFilteredRoleService";
    final String attrName = "nsRoleFilter";
    final String schemaType = "Dynamic";
    final String value = "required|adminDisplay";

    /**
     * Updates the <code>any</code> attribute in the 
     * <code>sunIdentityFilteredRoleService</code> service schema.
     *
     * @return true if attribute change is successful otherwise error.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            UpgradeUtils.modifyAnyInAttributeSchema(
                    serviceName, null, schemaType,
                    attrName, value);
            isSuccess = true;
        } catch (UpgradeException e) {
        // log error.
        }

        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }
}
