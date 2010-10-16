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
 * $Id: Migrate.java,v 1.7 2008/10/06 06:15:12 bina Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Updates <code>sunAMDelegationService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "sunAMDelegationService";
    final static String SERVICE_DIR = "99_sunAMDelegationService/20_30";
    final static String ATTR_NAME = "SubjectIdTypes";
    final static String ATTR_NAME_1 = "attributes";
    final static String ATTR_NAME_2 = "notAttributes";

    final static String schemaType = "Global";
    final static String FILTERED_ROLE = "FILTERED_ROLE";
    final static String POLICY_NAME = "AllUserReadableServices";
    final static String POLICY_NAME_1 = "SelfWriteAttributes";
    final static String AGENTS_READ_WRITE = "AgentsReadWrite";
    final static String AGENT_ADMIN = "AgentAdmin";
    final static String PERMISSION = "Permission";
    final static String PRIVILEGE = "Privilege";
    final static String RESOURCE_SUFFIX1 = 
        "/sunFMSAML2MetadataService/1.0/OrganizationConfig/*";
    final static String RESOURCE_SUFFIX2 = 
        "/sunFMCOTConfigService/1.0/OrganizationConfig/*";
    final static String RULE_NAME_4 = "delegation-rule4";
    final static String RULE_NAME_5 = "delegation-rule5";
    final static String DEFAULT_VAL = 
        "iplanet-am-user-password-reset-force-reset";
    final static String CONDITION_NAME = "condition";
    /**
     * Updates the <code>sunAMDelegationService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            Set defaultValues = new HashSet();
            defaultValues.add(FILTERED_ROLE);
            UpgradeUtils.addAttributeDefaultValues(
                    SERVICE_NAME, null, schemaType,
                    ATTR_NAME, defaultValues);

            // add delegation rules
            Map ruleMap = new HashMap();
            ruleMap.put(RULE_NAME_4,RESOURCE_SUFFIX1);
            ruleMap.put(RULE_NAME_5,RESOURCE_SUFFIX2);
            UpgradeUtils.addDelegationRule(POLICY_NAME,ruleMap); 

            // remove default value
            Map removePasswordResetForceChangePwd = new HashMap(2);
            Set setPwdReset = new HashSet(2);
            setPwdReset.add(DEFAULT_VAL);
            removePasswordResetForceChangePwd.put("condition", setPwdReset);
            UpgradeUtils.removeDelegationCondition(POLICY_NAME_1,
                 ATTR_NAME_1,removePasswordResetForceChangePwd);

            // FOR ISSUE 3548
            UpgradeUtils.removeDelegationPolicyAttribute(
                POLICY_NAME_1,ATTR_NAME_2,CONDITION_NAME);

            // add AgentsReadWrite Permission
            Map attrValues = new HashMap();
            Set attrValuesSet = new HashSet();
            attrValuesSet.add("*REALM" +
                "/sunIdentityRepositoryService/1.0/application/agent*");
            attrValues.put("resource",attrValuesSet);

            Set actionsSet = new HashSet();
            actionsSet.add("READ");
            actionsSet.add("MODIFY");
            actionsSet.add("DELEGATE");
            attrValues.put("actions",actionsSet);
            UpgradeUtils.addSubConfig(SERVICE_NAME,"Permissions",
                AGENTS_READ_WRITE,PERMISSION,attrValues,0);

            attrValues.clear();
            attrValuesSet.clear();

            attrValuesSet.add("AgentsReadWrite");
            attrValues.put("listOfPermissions",attrValuesSet);

            UpgradeUtils.addSubConfig(SERVICE_NAME,"Privileges",
                AGENT_ADMIN,PRIVILEGE,attrValues,0);

            UpgradeUtils.createOrganizationConfiguration(SERVICE_NAME,"/",null);
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
