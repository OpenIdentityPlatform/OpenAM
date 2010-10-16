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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:53:43 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Updates <code>iPlanetAMPolicyConfigService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMPolicyConfigService";
    final static String SERVICE_DIR = "50_iPlanetAMPolicyConfigService/20_30";
    final static String SCHEMA_FILE = "amPolicyConfig_mod.xml";
    final static String conditionAttrName =
            "iplanet-am-policy-selected-conditions";
    final static String conditionAttrValue = "SessionPropertyCondition";
    final static String subjectAttrName = "iplanet-am-policy-selected-subjects";
    final static String subjectAttrValue = "AMIdentitySubject";

    /**
     * Updates the <code>iPlanetAMPolicyConfigService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.importServiceData(fileName);

            // add default choice values to the attributes in the realm.
            Set attrValues = new HashSet();
            attrValues.add(conditionAttrValue);
            UpgradeUtils.addAttributeValuesToRealms(
                    SERVICE_NAME, conditionAttrName, attrValues);
            attrValues.clear();
            attrValues.add(subjectAttrValue);
            UpgradeUtils.addAttributeValuesToRealms(
                    SERVICE_NAME, subjectAttrName, attrValues);
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
