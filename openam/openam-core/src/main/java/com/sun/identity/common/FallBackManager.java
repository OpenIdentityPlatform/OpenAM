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
 * $Id: FallBackManager.java,v 1.6 2009/11/04 22:49:09 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.common;

import java.util.*;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.event.EventService;


public class FallBackManager extends GeneralTaskRunnable {

    public static Debug debug = Debug.getInstance("LDAPConnectionPool");

    // 1 sec = 1000 milliseconds. 5 * 60 seconds * 1000 = 5 minutes 
    private static final long DEFAULT_SERVER_CHECK_INTERVAL = 15;
    private long sleepTime = DEFAULT_SERVER_CHECK_INTERVAL;
    private volatile long runPeriod;

    public FallBackManager() {

    /*
     * Gets the time interval from AMConfig.properties set into the key
     * "com.sun.am.ldap.fallback.sleep.minutes" and
     * sets the time interval to check whether primary is up to fallback to
     * happen. If it is not set in the properties file,
     * default value of 15 minutes is set.
     * 1 sec = 1000 milliseconds. 15min * 60 seconds * 1000 = 60000.
     */

        String sleepTimeStr =
            SystemProperties.get(Constants.LDAP_FALLBACK_SLEEP_TIME_IN_MINS);
        if (debug.messageEnabled()) {
            debug.message("FallBackManager:constructor().sleepTime in minutes."
                + sleepTimeStr);
        }
        if (sleepTimeStr != null && sleepTimeStr.length() > 0) {
            try {
                sleepTime = Long.parseLong(sleepTimeStr);
            } catch(NumberFormatException nex) {
                if (debug.messageEnabled()) {
                    debug.message("Server Check Interval is not set\n"+
                        "for fallback. Setting it to default value 15 min");
                }
            }
        }
        sleepTime = (sleepTime*60000);
        if (debug.messageEnabled()) {
            debug.message("FallBackManager:constructor().sleepTime in seconds."
                + sleepTime);
        }
    }

    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return the run period of this task.
     */
    public long getRunPeriod() {
        return runPeriod;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean addElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean removeElement(Object obj) {
        return false;
    }
    
    /**
     * Implements for GeneralTaskRunnable.
     *
     * @return false since this class will not be used as container.
     */
    public boolean isEmpty() {
        return true;
    }
    
    public void run() {
        boolean foundDown = false;
        try {
            if ( (LDAPConnPoolUtils.connectionPoolsStatus != null)
                && (!LDAPConnPoolUtils.connectionPoolsStatus.isEmpty()) ) {
                    Set keyset1 = LDAPConnPoolUtils.connectionPoolsStatus
                        .keySet();
                    Iterator iter1 = keyset1.iterator();
                    while (iter1.hasNext()) {
                        String key = (String)iter1.next();
                        LDAPConnectionPool fbConn  = 
                            (LDAPConnectionPool)LDAPConnPoolUtils.
                                connectionPoolsStatus.get(key);
                        foundDown =true;
                        StringTokenizer st = new StringTokenizer(key,":");
                        String downName = (String)st.nextToken();
                        String downHost = (String)st.nextToken();
                        String downPort = (String)st.nextToken();
                        if (debug.messageEnabled()) {
                            debug.message("FallBackManager:Checking for server"+
                                ":- "+ key);
                            debug.message("FallBackManager:downHost "+downHost);
                            debug.message("FallBackManager:downPort "+downPort);
                        }
                        if ((downHost != null) && (downHost.length() != 0)
                            && (downPort != null) && (downPort.length()!= 0)) {
                            int intPort = (Integer.valueOf(downPort))
                                .intValue();
                            try {
                                LDAPConnection ldapConn = new LDAPConnection();
                                ldapConn.connect(downHost, intPort);
                                if (ldapConn.isConnected()) {
                                    fbConn.fallBack(ldapConn);                                    
                                    // To facilitate event service/persistant 
                                    // notifications to fallback
                                    // Instantiate EventService
                                    // Then get the service thread of this 
                                    // instance and interrupt,
                                    // so that event service resets everything.
                                    try {
                                        EventService.getEventService().
                                            resetAllSearches(false);
                                    } catch (Exception ee) {
                                        debug.error("FallBackManager: Error " +
                                            "while interrupting the event" + 
                                            "service.", ee);
                                    }
                                }
                                ldapConn.disconnect();
                            } catch ( LDAPException e ) {
                            }
                        }
                    }
            }
        } catch (Exception exp) {
            debug.error("FallBackManager:Error in FallBack Manager Thread",
                exp);
        }
        if (!foundDown) {
            runPeriod = -1;
            setHeadTask(null);
            debug.message("Returning back from FallBackManager thread."+
                foundDown);
            debug.message("exiting FallBackManager thread..");
        } else {
            runPeriod = sleepTime;
        }
    } //end of thread - run()
}
