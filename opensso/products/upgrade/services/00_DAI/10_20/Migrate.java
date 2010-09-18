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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:53:10 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeUtils;
import com.sun.identity.upgrade.UpgradeException;

/**
 * Migrates the DAI Service.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final String SERVICE_NAME = "DAI";
    final String SERVICE_DIR = "00_DAI/10_20";
    final String SCHEMA_FILE = "ums_addschema.xml";
    final String LDIF_FILE = "DAI_ds_remote_schema.ldif";

    /**
     * Loads the ldif and schema changes for DAI service.
     *
     * @return true if successful else false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
        //load ldif file
        UpgradeUtils.loadLdif(
                UpgradeUtils.getAbsolutePath(SERVICE_DIR, LDIF_FILE));
        String fileName =
                UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
        UpgradeUtils.importServiceData(fileName);
        isSuccess = true;
        } catch (UpgradeException e) {
        UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME,e);
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
