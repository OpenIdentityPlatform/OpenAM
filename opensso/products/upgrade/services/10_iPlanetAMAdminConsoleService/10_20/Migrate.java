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
 * $Id: Migrate.java,v 1.3 2008/06/25 05:53:13 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;

/**
 * Migrates the <code>iPlanetAMAdminConsoleService</code>.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    static final String SERVICE_NAME = "iPlanetAMAdminConsoleService";
    static final String SERVICE_DIR = "10_iPlanetAMAdminConsoleService/10_20/";
    static final String SCHEMA_TYPE = "Organization";
    static final String LDIF_FILE = "amAdminConsole_install.ldif";
    static final String INVALID_CHARS_ATTR =
            "iplanet-am-admin-console-invalid-chars";
    static final String USER_PASSWD_CLASS_ATTR =
            "iplanet-am-admin-console-user-password-validation-class";
    static final String SCHEMA_FILE1 = "amAdminConsole_mod.xml";
    static final String SCHEMA_FILE2 = "amAdminConsole_addDefaultVal.xml";

    /**
     * Loads the ldif and service schema changes for 
     * <code>iPlanetAMAdminConsoleService</code> service.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            // if the attribute iplanet-am-admin-console-invalid-chars
            // exists then is 6.3 DIT and don't need to update the
            // data since its already there.

            if (UpgradeUtils.attributeExists(
                    SERVICE_NAME, INVALID_CHARS_ATTR, SCHEMA_TYPE)) {
                UpgradeUtils.loadLdif(
                        UpgradeUtils.getAbsolutePath(SERVICE_DIR, LDIF_FILE));
                // get attribute value for
                // iplanet-am-admin-console-user-password-validation-class
                // if it already has a value don't add the default value.
                if (UpgradeUtils.attributeExists(
                        SERVICE_NAME, USER_PASSWD_CLASS_ATTR, SCHEMA_TYPE)) {
                    UpgradeUtils.importServiceData(UpgradeUtils.getAbsolutePath(
                            SERVICE_DIR, SCHEMA_FILE1));
                } else {
                    String[] fileList = new String[2];
                    fileList[0] =
                            UpgradeUtils.getAbsolutePath(
                            SERVICE_DIR, SCHEMA_FILE1);
                    fileList[1] =
                            UpgradeUtils.getAbsolutePath(
                            SERVICE_DIR, SCHEMA_FILE2);
                    UpgradeUtils.importServiceData(fileList);
                }
                isSuccess = true;
            }
        } catch (UpgradeException ue) {
            UpgradeUtils.debug.error("Error loading schema ", ue);
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
