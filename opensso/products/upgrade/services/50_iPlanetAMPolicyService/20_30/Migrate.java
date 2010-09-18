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
 * $Id: Migrate.java,v 1.3 2008/06/25 05:53:46 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.ArrayList;
import java.util.List;

public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMPolicyService";
    final static String SERVICE_DIR = "50_iPlanetAMPolicyService/20_30";
    final static String[] SCHEMA_FILE_LIST =
            {"amPolicy_mod.xml", "SetPVUForAU.xml", "SetPVUrls.xml"};

    final static String[] NEW_SCHEMA_FILE_LIST = {"AddAMIdentitySubject.xml",
        "AddIDResponseProvider.xml",
        "AddUserSelfCheckCondition.xml",
        "AddSessionPropertyCondition.xml"
    };

    /**
     * Updates the <code>iPlanetAMPolicyService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            List schemaList = new ArrayList();
            String fileName = null;
            //String[] fileList = new String[3];
            for (int i = 0; i < 3 ; i++) {
                fileName =
                        UpgradeUtils.getAbsolutePath(
                        SERVICE_DIR, SCHEMA_FILE_LIST[i]);
                schemaList.add(fileName);
            }
            UpgradeUtils.importServiceData(schemaList);
            List newSchemaList = new ArrayList();
            //fileList = new String[4];
            for (int i = 0; i < 4 ; i++) {
                fileName = UpgradeUtils.getAbsolutePath(
                        SERVICE_DIR, NEW_SCHEMA_FILE_LIST[i]);
                newSchemaList.add(fileName);
            }
            UpgradeUtils.importNewServiceSchema(newSchemaList);
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
