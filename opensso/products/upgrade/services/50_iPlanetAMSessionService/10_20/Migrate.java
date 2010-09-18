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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:53:49 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;

/**
 * Updates <code>iPlanetAMAuthCertService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMSessionService";
    final static String SERVICE_DIR = "50_iPlanetAMSessionService/10_20";
    final static String SCHEMA_FILE = "amSession_add.xml";
    final static String NEW_SCHEMA_FILE = "amSession_addConfig.xml";

    /**
     * Updates the <code>iPlanetAMSessionService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.importServiceData(fileName);
            // TODO: check with SM team :  load empty config
            // doing this since there's a bug in SMS
            // which requires an empty configuration
            // in order to be able to add subconfigurations.
            fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, NEW_SCHEMA_FILE);
            UpgradeUtils.importNewServiceSchema(fileName);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
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
