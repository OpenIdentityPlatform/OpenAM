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
 * $Id: Migrate.java,v 1.2 2008/06/25 05:53:33 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashSet;
import java.util.Set;
import java.io.File;

/**
 * Updates <code>iPlanetAMAuthSecureIDService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMAuthSecurIDService";
    final static String SUB_SCHEMA = "serverconfig";

    final static String CONFIG_PATH_ATTR =
        "iplanet-am-auth-securid-server-config-path";
    final static String CONFIG_PORT_ATTR =
        "iplanet-am-auth-securid-config-port";
    final static String HELPER_PORT_ATTR =
        "iplanet-am-auth-securid-helper-port";

    final static String schemaType = "Organization";


    /**
     * Updates the <code>iPlanetAMAuthSecurIDService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            /*
             * remove the i18nKey for iplanet-am-auth-securid-config-port and
             * iplanet-am-auth-securid-helper-port, so they aren't visible
             */

            UpgradeUtils.modifyI18NInAttributeSchema(SERVICE_NAME,
                null, schemaType, CONFIG_PORT_ATTR, null);
            UpgradeUtils.modifyI18NInAttributeSchema(SERVICE_NAME,
                null, schemaType, HELPER_PORT_ATTR, null);

            //modify in serverconfig subschema
            UpgradeUtils.modifyI18NInAttributeSchema(SERVICE_NAME,
                SUB_SCHEMA, schemaType, CONFIG_PORT_ATTR, null);
            UpgradeUtils.modifyI18NInAttributeSchema(SERVICE_NAME,
                SUB_SCHEMA, schemaType, HELPER_PORT_ATTR, null);

            // now update the server-config-path attribute
            String configDir = UpgradeUtils.getConfigDir();
            String deployURI = UpgradeUtils.getDeployURI();
            String aceConfigPath = configDir + File.separator + deployURI +
                File.separator + "auth/ace/data";

            // remove the default /opt/ace/data, if it's still /opt/ace/data
            modifyConfigPath(null,aceConfigPath);
            modifyConfigPath(SUB_SCHEMA,aceConfigPath);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }

        return isSuccess;
    }

    static void modifyConfigPath(String subSchema,String aceConfigPath)
            throws UpgradeException
    {
        String temps = null; 
        if (subSchema != null) {
            temps = UpgradeUtils.getSubSchemaAttributeValue(
                SERVICE_NAME, schemaType, CONFIG_PATH_ATTR,subSchema);
        } else {
            temps = UpgradeUtils.getAttributeValueString(SERVICE_NAME,
            CONFIG_PATH_ATTR, schemaType);
        }
        if ((temps != null) && (temps.length() > 0)) {
            if (temps.trim().equals("/opt/ace/data")) {
                /*
                 * create <configdir>/<uri>/auth/ace/data
                 * if it doesn't already exist
                 */
                File acPath = new File(aceConfigPath);
                if (!acPath.exists()) {
                    acPath.mkdirs();
                }
                Set defaultValue = new HashSet();
                defaultValue.add("/opt/ace/data");
                UpgradeUtils.removeAttributeDefaultValues(SERVICE_NAME,
                    schemaType, CONFIG_PATH_ATTR, defaultValue, subSchema);

                // add the new <configdir>/<uri>/auth/ace/data
                defaultValue.clear();
                defaultValue.add(aceConfigPath);
                UpgradeUtils.addAttributeDefaultValues(SERVICE_NAME,
                    subSchema, schemaType, CONFIG_PATH_ATTR, defaultValue);
            }
        }
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
