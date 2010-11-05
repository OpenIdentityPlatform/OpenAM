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
 * $Id: SMSPropertiesObserver.java,v 1.1 2008/07/30 00:50:15 arviranga Exp $
 *
 */

package com.sun.identity.sm;

import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.shared.debug.Debug;

/**
 * Listenes to changes to <class>SystemProperties</class> and reinitialized
 * configuration framework. The properties that takes effect upon change are:
 * com.iplanet.am.sdk.caching.enabled
 * com.sun.identity.sm.cache.enabled
 * com.sun.identity.sm.enableDataStoreNotification
 * com.sun.identity.sm.cache.ttl.enable
 * "com.sun.identity.sm.cache.ttl
 */
public class SMSPropertiesObserver implements ConfigurationListener {
    private static SMSPropertiesObserver instance;
    private static Debug debug = Debug.getInstance("amSMS");
    
    private SMSPropertiesObserver() {
        // Private constructor
    }
    
    public synchronized static SMSPropertiesObserver getInstance() {
        if (instance == null) {
            instance = new SMSPropertiesObserver();
            // Register the listener with ConfigurationObserver
            ConfigurationObserver.getInstance().addListener(instance);
            if (debug.messageEnabled()) {
                debug.message("SMSPropertiesObserver:getInstance Instantiated" +
                    "and added to ConfigurationObserver");
            }
        }
        return instance;
    }

    public void notifyChanges() {
        if (debug.messageEnabled()) {
            debug.message("SMSPropertiesObserver:notifyChanges Received " +
                "change notifications from ConfigurationObserver");
        }
        // Initialize the system properties
        SMSEntry.initializeProperties();
        SMSNotificationManager.getInstance().initializeProperties();
        CachedSMSEntry.initializeProperties();
        SMSThreadPool.initialize(true);
    }
}
