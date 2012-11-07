/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuditLogFileNameTask.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * This task creates a unique name for the audit log file to be used by
 * the product instance being configured. The inputs to this task are the 
 * following:
 * <ul>
 * <li><code>PRODUCT_HOSTNAME_LOOKUP_KEY</code>: Key for product host</li>
 * <li><code>PRODUCT_PORT_LOOKUP_KEY</code>: Key for product port</li>
 * <li><code>PRODUCT_LOGFILE_NAME_OUTPUT_KEY</code>: Key for storing the 
 * generated file name.</li>
 * </ul>
 */
public class AuditLogFileNameTask implements ITask {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        boolean result = false;
        try {
            String productHostKey = (String) 
                properties.get(STR_PRODUCT_HOSTNAME_LOOKUP_KEY);
            String productPortKey = (String) 
                properties.get(STR_PRODUCT_PORT_LOOKUP_KEY);
            String outputKey = (String) 
                properties.get(STR_PRODUCT_LOGFILE_NAME_OUTPUT_KEY);

            String productHost = getRequiredValue(stateAccess, productHostKey,
                   STR_PRODUCT_HOSTNAME_LOOKUP_KEY);
            String productPort = getRequiredValue(stateAccess, productPortKey,
                   STR_PRODUCT_PORT_LOOKUP_KEY);
            validateKey(outputKey, STR_PRODUCT_LOGFILE_NAME_OUTPUT_KEY);

            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < productHost.length(); i++) {
                char ch = productHost.charAt(i);
                switch (ch) {
                case '.':
                case '-':
                    buffer.append('_');
                    break;
                default:
                    buffer.append(ch);
                    break;
                }
            }

            String fileName = STR_AUDIT_FILENAME_PREFIX + buffer.toString()
                    + '_' + productPort + STR_AUDIT_FILENAME_SUFFIX;
            Debug.log("AuditLogFileNameTask: Generated filename is: "
                    + fileName);
            stateAccess.put(outputKey, fileName);
            result = true;
        } catch (Exception ex) {
            throw new InstallException(LocalizedMessage
                    .get(LOC_TSK_ERR_AUDIT_LOGFILENAME), ex);
        }
        return result;
    }

    public boolean rollBack(String name, IStateAccess state, Map properties)
            throws InstallException {
        // No handling required
        return true;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        return LocalizedMessage.get(LOC_TSK_MSG_AUDIT_LOGFILE_EXECUTE);
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        return LocalizedMessage.get(LOC_TSK_MSG_AUDIT_LOGFILE_ROLLBACK);
    }

    private void validateKey(String key, String argument)
            throws IllegalArgumentException {
        if (key == null || key.trim().length() == 0) {
            Debug.log("AuditLogFileNameTask: No key specified for argument: "
                    + argument);
            throw new IllegalArgumentException(argument + "=" + key);
        }
    }

    private String getRequiredValue(IStateAccess state, String key,
            String argument) throws IllegalArgumentException {
        validateKey(key, argument);
        String result = (String) state.get(key);
        if (result == null || result.trim().length() == 0) {
            Debug.log("AuditLogFileNameTask: No value specified for key: "
                    + key + ", arg: " + argument);
            throw new IllegalArgumentException(key + "=" + result);

        }
        return result;
    }

    public static final String STR_PRODUCT_HOSTNAME_LOOKUP_KEY = 
        "HOSTNAME_LOOKUP_KEY";

    public static final String STR_PRODUCT_PORT_LOOKUP_KEY = "PORT_LOOKUP_KEY";

    public static final String STR_PRODUCT_LOGFILE_NAME_OUTPUT_KEY = 
        "LOGFILE_NAME_OUTPUT_KEY";

    public static final String STR_AUDIT_FILENAME_PREFIX = "am"
            + ToolsConfiguration.getProductShortName() + "_";

    public static final String STR_AUDIT_FILENAME_SUFFIX = ".log";

    // Localiziation keys
    public static final String LOC_TSK_ERR_AUDIT_LOGFILENAME = 
        "TSK_ERR_AUDIT_LOGFILENAME";

    public static final String LOC_TSK_MSG_AUDIT_LOGFILE_EXECUTE = 
        "TSK_MSG_AUDIT_LOGFILE_EXECUTE";

    public static final String LOC_TSK_MSG_AUDIT_LOGFILE_ROLLBACK = 
        "TSK_MSG_AUDIT_LOGFILE_ROLLBACK";
}
