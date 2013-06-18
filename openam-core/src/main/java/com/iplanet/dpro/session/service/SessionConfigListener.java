/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionConfigListener.java,v 1.7 2008/08/19 19:08:39 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.dpro.session.service;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Map;
import java.util.Set;

/**
 * This class implements the interface <code>ServiceListener</code> in order
 * to receive session service data change notifications. The method
 * schemaChanged() is invoked when a session service schema data under
 * followings has been changed. Console/Service Configuration/OpenSSO
 * Configuration/Session - Upper limit for session search result set size -
 * Search timeout
 * @see com.sun.identity.sm.ServiceSchemaManager
 */
public class SessionConfigListener implements ServiceListener {
    private static String SESSION_SERVICE_NAME = "iPlanetAMSessionService";

    private static final String SESSION_RETRIEVAL_TIMEOUT = 
        "iplanet-am-session-session-list-retrieval-timeout";

    private static final String MAX_SESSION_LIST_SIZE = 
        "iplanet-am-session-max-session-list-size";    

    private static final String SESSION_CONSTRAINT = 
        "iplanet-am-session-enable-session-constraint";
    
    private static final String DENY_LOGIN_IF_DB_IS_DOWN =
        "iplanet-am-session-deny-login-if-db-is-down";
    
    private static final String CONSTRAINT_HANDLER =
        "iplanet-am-session-constraint-handler";

    private static final String MAX_WAIT_TIME_FOR_CONSTRAINT = 
        "iplanet-am-session-constraint-max-wait-time";    

    private static long defSessionRetrievalTimeout;

    private static int defMaxSessionListSize;

    private static Debug debug = Debug.getInstance("amSession");

    private static ServiceSchemaManager sSchemaMgr = null;

    public static long defSessionRetrievalTimeoutLong = 5;

    public static int defMaxSessionListSizeInt = 200;

    public static String defSessionRetrievalTimeoutStr = Long
            .toString(defSessionRetrievalTimeoutLong);

    public static String defMaxSessionListSizeStr = Integer
            .toString(defMaxSessionListSizeInt);

    private static String enablePropertyNotificationStr = "OFF";     
    
    public static String defmaxWaitTimeForConstraintStr = "6000"; // in milli-second

    
   /**
    * Creates a new SessionConfigListener
    * @param ssm ServiceSchemaManager
    */
    public SessionConfigListener(ServiceSchemaManager ssm) {
        sSchemaMgr = ssm;
    }

    /**
     * This method is used to receive notifications if schema changes.
     * 
     * @param serviceName
     *            the name of the service.
     * @param version
     *            the version of the service. this method is No-op.
     */
    public void schemaChanged(String serviceName, String version) {
        if ((serviceName != null)
                && !serviceName.equalsIgnoreCase(SESSION_SERVICE_NAME)) {
            return;
        }

        try {
            ServiceSchema schema = sSchemaMgr.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            defSessionRetrievalTimeoutStr = CollectionHelper.getMapAttr(attrs,
                    SESSION_RETRIEVAL_TIMEOUT, defSessionRetrievalTimeoutStr);
            defMaxSessionListSizeStr = CollectionHelper.getMapAttr(attrs,
                    MAX_SESSION_LIST_SIZE, defMaxSessionListSizeStr);
            enablePropertyNotificationStr = CollectionHelper.getMapAttr(attrs,
                    Constants.PROPERTY_CHANGE_NOTIFICATION, "OFF");

            if (enablePropertyNotificationStr.equalsIgnoreCase("ON")) {
                SessionService.setPropertyNotificationEnabled(true);
                Set notProp = (Set) attrs
                        .get(Constants.NOTIFICATION_PROPERTY_LIST);
                SessionService.setNotificationProperties(notProp);
            } else {
                SessionService.setPropertyNotificationEnabled(false);
            }

            Set<String> timeoutHandlers = (Set<String>) attrs
                    .get(Constants.TIMEOUT_HANDLER_LIST);
            SessionService.setTimeoutHandlers(timeoutHandlers);

            String enableSessionTrimmingStr = CollectionHelper.getMapAttr(
                    attrs, Constants.ENABLE_TRIM_SESSION, "NO");
            if (enableSessionTrimmingStr.equalsIgnoreCase("YES")) {
            	SessionService.setSessionTrimmingEnabled(true);
            } else {
            	SessionService.setSessionTrimmingEnabled(false);
            }
            
            String enableQuotaconstraintsStr = CollectionHelper.getMapAttr(
                    attrs, SESSION_CONSTRAINT, "OFF");
            if (enableQuotaconstraintsStr.equalsIgnoreCase("ON")) {
            	SessionService.setSessionConstraintEnabled(true);
            } else {
            	SessionService.setSessionConstraintEnabled(false);
            }
            
            String denyLoginStr = CollectionHelper.getMapAttr(attrs,
                    DENY_LOGIN_IF_DB_IS_DOWN, "NO");
            if (denyLoginStr.equalsIgnoreCase("YES")) {
                SessionService.setDenyLoginIfDBIsDown(true);
            } else {
            	SessionService.setDenyLoginIfDBIsDown(false);
            }

            SessionService.setConstraintHandler(CollectionHelper.getMapAttr(
                    attrs, CONSTRAINT_HANDLER,
                    SessionConstraint.DESTROY_OLDEST_SESSION_CLASS));

            defmaxWaitTimeForConstraintStr = CollectionHelper.getMapAttr(attrs,
                MAX_WAIT_TIME_FOR_CONSTRAINT, defmaxWaitTimeForConstraintStr);            

        } catch (Exception e) {
            debug.error("SessionConfigListener : "
                    + "Unable to get Session Service attributes", e);
        }

        try {
            defSessionRetrievalTimeout = Long
                    .parseLong(defSessionRetrievalTimeoutStr) * 1000;
        } catch (Exception e) {
            defSessionRetrievalTimeout = defSessionRetrievalTimeoutLong * 1000;
            debug.error(
                    "SessionConfigListener : Unable to parse Timeout values",
                            e);
        }

        try {
            defMaxSessionListSize = Integer.parseInt(defMaxSessionListSizeStr);
        } catch (Exception e) {
            defMaxSessionListSize = defMaxSessionListSizeInt;
            debug.error(
                    "SessionConfigListener : Unable to parse ListSize values",
                    e);
        }
        
        int maxWaitTimeForConstraint = 6000;
        try {
            maxWaitTimeForConstraint = Integer.parseInt(
                                           defmaxWaitTimeForConstraintStr);
            SessionService.setMaxWaitTimeForConstraint(
                                           maxWaitTimeForConstraint);
        } catch (Exception e) {
            SessionService.setMaxWaitTimeForConstraint(
                                           maxWaitTimeForConstraint);
            debug.message(
                "SessionConfigListener : Unable to parse Wait Time values." +
                " Defaulting to 6000 milli seconds",e);
        }        
    }

    /**
     * This method for implementing ServiceListener. As this object listens for
     * changes in schema of amConsoleService. this method is No-op.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     * @param groupName
     *            name of the group
     * @param serviceComponent
     *            service component
     * @param type
     *            type of modification
     */
    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        // No op.
    }

    /**
     * This method for implementing ServiveListener. As this object listens for
     * changes in schema of amConsoleService. this method is No-op.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     * @param orgName
     *            name of the org
     * @param groupName
     *            name of the group
     * @param serviceComponent
     *            service component
     * @param type
     *            type of modification this method is No-op.
     */

    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        // No op.
    }

    /**
     * Retrieves Timeout of Session search.
     * @return timeout for search
     */
    public static long getTimeout() {
        return defSessionRetrievalTimeout;
    }

    /**
     * Gets Max list size of Session search.
     * @return maximum size of sessions
     */
    public static int getMaxsize() {
        return defMaxSessionListSize;
    }
}
