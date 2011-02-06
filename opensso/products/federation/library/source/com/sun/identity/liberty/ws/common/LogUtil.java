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
 * $Id: LogUtil.java,v 1.4 2008/08/06 17:28:07 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.common;

import java.util.logging.Level;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogManager;
import com.sun.identity.shared.Constants;

/**
 * The <code>LogUtil</code> class defines methods which are used by
 * Liberty compoment to write logs.
 */
public class LogUtil {

    /**
     * Success msgid.
     */
    public static final String WS_SUCCESS = "WS_Success";

    /**
     * Failure msgid.
     */
    public static final String WS_FAILURE = "WS_Failure";
  
    /**
     * Authentication Service OK msgid. 
     */
    public static final String AS_OK = "AS_OK";

    /**
     * Authentication Service Continue msgid.
     */
    public static final String AS_CONTINUE = "AS_Continue";

    /**
     * Authentication Service Abort msgid.
     */
    public static final String AS_ABORT = "AS_Abort";

    /**
     * PP Query Success msgid.
     */
    public static final String PP_QUERY_SUCCESS = "PP_Query_Success";

    /**
     * PP Query Failure msgid. 
     */
    public static final String PP_QUERY_FAILURE = "PP_Query_Failure";

    /**
     * PP Modify Failure msgid.
     */
    public static final String PP_MODIFY_SUCCESS = "PP_Modify_Success";

    /**
     * PP Modify Failure msgid. 
     */
    public static final String PP_MODIFY_FAILURE = "PP_Modify_Failure";

    /**
     * PP Interaction Success msgid.
     */
    public static final String PP_INTERACTION_SUCCESS = 
                               "PP_Interaction_Success";

    /**
     * PP Interaction Failure msgid.
     */
    public static final String PP_INTERACTION_FAILURE = 
                               "PP_Interaction_Failure";

    /**
     * Discovery Service Lookup Success msgid.
     */
    public static final String DS_LOOKUP_SUCCESS = "DS_Lookup_Success";


    /**
     * Discovery Service Lookup Failure msgid.
     */
    public static final String DS_LOOKUP_FAILURE = "DS_Lookup_Failure";

    /**
     * Discovery Service Update Success msgid.
     */
    public static final String DS_UPDATE_SUCCESS = "DS_Update_Success";

    /**
     * Discovery Service Update Failure msgid.
     */
    public static final String DS_UPDATE_FAILURE = "DS_Update_Failure";

    /**
     * IneractionManager sending message to SP.
     */
    public static final String IS_SENDING_MESSAGE
            = "IS_Sending_Message";
    /**
     * IneractionManager resending message to SP.
     */
    public static final String IS_RESENDING_MESSAGE
            = "IS_Resending_Message";
    /**
     * IneractionManager returning response message.
     */
    public static final String IS_RETURNING_RESPONSE_MESSAGE
            = "IS_Returning_Response_Message";
    /**
     * IneractionManager redirected user agent to interaction service.
     */
    public static final String IS_REDIRECTED_USER_AGENT
            = "IS_Redirected_User_Agent";

    /**
     * IneractionManager redirected user agent back to SP.
     */
    public static final String IS_REDIRECTED_USER_AGENT_BACK 
            = "IS_Redirected_User_Agent_Back";

    /**
     * InteractionService presented query page to user agent.
     */
    public static final String IS_PRESENTED_QUERY_TO_USER_AGENT
            = "IS_Presented_Query_To_User_Agent";

    /**
     * Interaction service collected response from user agent.
     */
    public static final String IS_COLLECTED_RESPONSE_FROM_USER_AGENT
            = "IS_Collected_Response_From_User_Agent";

    /**
     * Interaction service returning response element.
     */
    public static final String IS_RETURNING_RESPONSE_ELEMENT
            = "IS_Returning_Response_Element";

    private static final String LIBERTY_LOG = "Liberty";
    private static Debug debug = Debug.getInstance("libIDWSF");
    private static Logger logger = null;
    private static boolean logStatus = false;

    static {
        String status = SystemPropertiesManager.get(Constants.AM_LOGSTATUS,
            "INACTIVE");

        if (status.equalsIgnoreCase("ACTIVE")) {
            logStatus = true;
            try {
                logger = LogManager.getLogger(LIBERTY_LOG);
            } catch (LogException le) {
                debug.error("LogUtil.static:", le);
            }
        }
    }

    /**
     * Writes access to Liberty components to a log (amLiberty.access).
     * @param level java.util.logging.Level indicating log level
     * @param msgid Message id
     * @param data Message to be logged.
     */
    public static void access(Level level, String msgid,String[] data) {

        if ((logger != null) && logger.isAccessLoggable(level)) {
            try {
                logger.access(level, msgid, data, null);
            } catch (LogException le) {
                debug.error("LogUtil.access:", le);
            }
        }
    }

    /**
     * Writes error occurred in Liberty components to a log (amLiberty.error).
     * @param level java.util.logging.Level indicating log level
     * @param msgid Message id
     * @param data Message to be logged.
     */
    public static void error(Level level, String msgid,String[] data) {

        if ((logger != null) && logger.isErrorLoggable(level)) {
            try {
                logger.error(level, msgid, data, null);
            } catch (LogException le) {
                debug.error("LogUtil.error:", le);
            }
        }
    }

    /**
     * Checks if the log is enabled.
     * @return true if enabled.
     */
    public static boolean isLogEnabled() {
        return logStatus;
    }
}
