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
 * $Id: Migrate.java,v 1.3 2008/06/25 05:53:39 qcheng Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.StringTokenizer;

/**
 * Updates <code>iPlanetAMLoggingService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMLoggingService";
    final static String SERVICE_DIR = "50_iPlanetAMLoggingService/30_40";
    final static String files = "remove_amLogging.xml add_amLogging.xml";
    final static String schemaType = "Global";
    final static String LOGGING_TYPE_ATTR = "iplanet-am-logging-type";
    final static String BUFF_TIME_ATTR =
            "iplanet-am-logging-buffer-time-in-seconds";
    final static String BUFF_SIZE_ATTR = "iplanet-am-logging-buffer-size";
    final static String BUFF_STATUS_ATTR =
            "iplanet-am-logging-time-buffering-status";

    final static String FILE_DB_TYPE = "File";
    final static String OFF_TIMER_STAT = "OFF";
    final static String BUFF_SIZE_ONE = "1";
    final static String BUFF_TIME = "3600";
    final static String SCHEMA_FILE = "modLogging.xml";

    /**
     * Updates the <code>iPlanetAMLoggingService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        String[] fileList;
        try {
            String dbLogging =
                    UpgradeUtils.getAttributeValueString(SERVICE_NAME,
                    LOGGING_TYPE_ATTR, schemaType);
            String buffTime =
                    UpgradeUtils.getAttributeValueString(SERVICE_NAME,
                    BUFF_TIME_ATTR, schemaType);
            String buffSize =
                    UpgradeUtils.getAttributeValueString(SERVICE_NAME,
                    BUFF_SIZE_ATTR, schemaType);
            String timerStat =
                    UpgradeUtils.getAttributeValueString(SERVICE_NAME,
                    BUFF_STATUS_ATTR, schemaType);
            int i = 0;
            if ((dbLogging != null && dbLogging.equals(FILE_DB_TYPE)) &&
                    (timerStat != null && timerStat.equals(OFF_TIMER_STAT)) &&
                    (buffSize != null && buffSize.equals(BUFF_SIZE_ONE)) &&
                    (buffTime != null && buffTime.equals(BUFF_TIME))) {
                fileList = new String[3];
                fileList[0] = UpgradeUtils.getAbsolutePath(
                        SERVICE_DIR, SCHEMA_FILE);
                i++;
            } else {
                fileList = new String[2];
            }
            StringTokenizer st = new StringTokenizer(files);
            while (st.hasMoreTokens()) {
                fileList[i] =
                        UpgradeUtils.getAbsolutePath(
                        SERVICE_DIR, (String) st.nextToken());
                i++;
            }
            UpgradeUtils.importServiceData(fileList);
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
