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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:53:43 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Migrates <code>iPlanetAMPolicyConfigService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMPolicyConfigService";
    final static String SERVICE_DIR = "50_iPlanetAMPolicyConfigService/10_20";
    final static String SCHEMA_TYPE = "Global";
    final static String ATTR_NAME =
            "iplanet-am-policy-config-resource-comparator";
    Set existingAttrVal = Collections.EMPTY_SET;
    final static String oldVal =
            "com.sun.identity.policy.plugins.URLResourceName";
    final static String newVal =
            "com.sun.identity.policy.plugins.HttpURLResourceName";

    /**
     * Migrates the <code>iPlanetAMAuthHTTPBasicService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            Iterator i = existingAttrVal.iterator();
            Set defaultVal = new HashSet();
            String existingVal = null;
            String newAttrVal = null;
            while (i.hasNext()) {
                existingVal = (String) i.next();
                newAttrVal = existingVal.replaceFirst(oldVal, newVal);
                defaultVal.add(newAttrVal);
            }
            UpgradeUtils.setAttributeDefaultValues(
                    SERVICE_NAME, null, SCHEMA_TYPE, ATTR_NAME, defaultVal);
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
        // retreive the existing attribute value
        boolean isSuccess = false;
        try {
            existingAttrVal =
                    UpgradeUtils.getAttributeValue(SERVICE_NAME,
                    ATTR_NAME, SCHEMA_TYPE);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }
        return isSuccess;
    }
}
