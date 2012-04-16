/* The contents of this file are subject to the terms
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
 * $Id: LogTestConstants.java,v 1.2 2008/04/11 04:49:31 kanduls Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.log;

import com.sun.identity.qatest.common.TestConstants;

/**
 * This interface contains the property keys as constants.
 */
public interface LogTestConstants {
    static final String LOGTEST_KEY_DESCRIPTION = "description";
    static final String LOGTEST_KEY_USER_ID = "user_id";
    static final String LOGTEST_KEY_USER_PASSWORD = "password";
    static final String LOGTEST_KEY_ATTR_VAL_PAIR = "attr_value_pair";
    static final String LOGTEST_KEY_FILE_NAME = "file_name";
    static final String LOGTEST_KEY_MESSAGE = "message";
    static final String LOGTEST_KEY_MODULE_NAME = "module_name";
    static final String LOGTEST_KEY_LOGGER_LEVEL = "logger_level";
    static final String LOGTEST_KEY_RECORD_LEVEL = "record_level";
    static final String LOGTEST_KEY_TABLE_NAME="table_name";
    static final String LOGTEST_KEY_LOG_LOCATION = 
            "iplanet-am-logging-location";
    static final String LOGTEST_KEY_DRIVER = "iplanet-am-logging-db-driver";
    static final String LOGTEST_KEY_DB_USER = "iplanet-am-logging-db-user";
    static final String LOGTEST_KEY_DB_PASSWORD = 
            "iplanet-am-logging-db-password";
    static final String LOGTEST_KEY_EXPECTED_MESSAGE = "expected_message";
    static final String LOGTEST_DB_CONF_FILE = "DBConfigInfo";
    static final String LOGTEST_KEY_LOGTYPE = "iplanet-am-logging-type";
    static final String LOGTEST_KEY_TIME_BUFFER_STATUS=
            "iplanet-am-logging-time-buffering-status";
    static final String LOGTEST_KEY_BUFF_SIZE = 
            "iplanet-am-logging-buffer-size";
}