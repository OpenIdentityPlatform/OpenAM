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
 * $Id: Migrate.java,v 1.4 2008/06/25 05:54:10 qcheng Exp $
 *
 */

import java.util.HashSet;
import java.util.Set;
import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;

/**
 * Updates <code>sunIdentityServerAuthnService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "sunIdentityServerAuthnService";
    final static String SERVICE_DIR = "99_sunIdentityServerAuthnService/20_30";
    final static String SCHEMA_FILE = "famAuthnSvc_addAttrs.xml";
    final static String PLAIN_MODULE =
        "com.sun.identity.liberty.authnsvc.plain.module";
    final static String CRAMMD5_MODULE =
        "com.sun.identity.liberty.authnsvc.crammd5.module";
    final static String ATTR_PLAIN_MECHANISM_AUTH_MODULE =
        "PlainMechanismAuthModule";
    final static String ATTR_CRAMMD5_MECHANISM_AUTH_MODULE =
        "CramMD5MechanismAuthModule";
    final static String schemaType = "Global";

    /**
     * Updates the <code>sunIdentityServerAuthnService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.addAttributeToSchema(
                    SERVICE_NAME, schemaType, fileName);

            // set i18n fileName
            UpgradeUtils.seti18NFileName(
                    SERVICE_NAME, "fmAuthnSvcConfiguration");

            String oldValue = UpgradeUtils.getServerProperties().getProperty(
                PLAIN_MODULE);

            if ((oldValue != null) && (oldValue.trim().length() > 0)) {
                Set defaultVals = new HashSet();
                defaultVals.add(oldValue);
                UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, null,
                    schemaType, ATTR_PLAIN_MECHANISM_AUTH_MODULE, defaultVals);
            }

            oldValue = UpgradeUtils.getServerProperties().getProperty(
                CRAMMD5_MODULE);

            if ((oldValue != null) && (oldValue.trim().length() > 0)) {
                Set defaultVals = new HashSet();
                defaultVals.add(oldValue);
                UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME, null,
                    schemaType, ATTR_CRAMMD5_MECHANISM_AUTH_MODULE,
                    defaultVals);
            }

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
