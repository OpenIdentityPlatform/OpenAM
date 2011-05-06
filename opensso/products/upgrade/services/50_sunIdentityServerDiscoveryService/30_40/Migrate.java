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
 * $Id: Migrate.java,v 1.3 2008/06/25 05:53:53 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Updates <code>sunIdentityServerDiscoveryService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "sunIdentityServerDiscoveryService";
    final static String SERVICE_DIR =
            "50_sunIdentityServerDiscoveryService/30_40";
    final static String SCHEMA_FILE = "famDisco_addAttrs.xml";
    final static String LDIF_FILE = "famDisco.ldif";
    final static String i18nFileName = "fmDiscoConfiguration";
    final static String ATTR_NAME = "serviceObjectClasses";
    final static String schemaType = "Global";
    final static String FM_DATA_STORE = "sunFederationManagerDataStore";

    /**
     * Updates the <code>sunIdentityServerDiscoveryService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            //load ldif file 
            String ldifPath =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, LDIF_FILE);
            UpgradeUtils.loadLdif(ldifPath);
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.addAttributeToSchema(
                    SERVICE_NAME, schemaType, fileName);
            // change i18NFileName attribute
            UpgradeUtils.seti18NFileName(SERVICE_NAME, i18nFileName);
            // change attribute default value
            Set defaultValues = new HashSet();
            defaultValues.add(FM_DATA_STORE);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, null,
                    schemaType, ATTR_NAME, defaultValues);
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
