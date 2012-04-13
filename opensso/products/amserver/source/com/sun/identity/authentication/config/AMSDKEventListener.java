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
 * $Id: AMSDKEventListener.java,v 1.6 2008/08/19 19:08:51 veiming Exp $
 *
 */


package com.sun.identity.authentication.config;

import com.iplanet.am.sdk.AMEvent;
import com.iplanet.am.sdk.AMEventListener;
import com.iplanet.am.sdk.AMObject;
import com.sun.identity.shared.debug.Debug;
import javax.security.auth.login.Configuration;

/**
 * Implements <code>AMEvenetListener</code> from OpenSSO SDK. This
 * listens to authentication configuration changes for users and roles
 */ 
public class AMSDKEventListener implements AMEventListener {

    private static Debug debug = Debug.getInstance("amAuthConfig");
    private String configName = null;
    private AMObject amObject = null;

    /**
     * Constructor.
     *
     * @param name Authentication configuration name
     */
    public AMSDKEventListener(String name) {
        configName = name;
    }

    /**
     * Returns configuration name.
     *
     * @return configuration name.
     */
    public String getConfigName() {
        return configName;
    }
 
    /**
     * Implements <code>com.iplanet.am.sdk.AMEventListener</code>.
     *
     * @param event
     * @see com.iplanet.am.sdk.AMEventListener#objectChanged
     */
    public void objectChanged(AMEvent event) {
        if (event == null) {
            debug.error("AMConfiguration.objectChanged, event null");
            // do nothing
            return;
        }
        if (debug.messageEnabled()) {
            debug.message("objectChanged, type=" + event.getEventType() +
                ", sDN=" + event.getSourceDN() + 
                ", sType=" + event.getSourceType());
        } 
        // process object change
        processSDKNotification();
    }

    /**
     * Implements <code>com.iplanet.am.sdk.AMEventListener</code>.
     *
     * @param event AM event 
     * @see com.iplanet.am.sdk.AMEventListener#objectRemoved
     */
    public void objectRemoved(AMEvent event) {
        if (event == null) {
            debug.error("AMConfiguration.objectChanged, event null");
            // do nothing
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("objectRemoved, type=" + event.getEventType() +
                ", sDN=" + event.getSourceDN() + 
                ", sType=" + event.getSourceType());
        } 
        // process object change
        processSDKNotification();
    }

    /**
     * Implements <code>com.iplanet.am.sdk.AMEventListener</code>.
     *
     * @param event AM event
     * @see com.iplanet.am.sdk.AMEventListener#objectRenamed
     */
    public void objectRenamed(AMEvent event) {
        if (event == null) {
            debug.error("AMConfiguration.objectChanged, event null");
            // do nothing
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("objectRenamed, type=" + event.getEventType() +
                ", sDN=" + event.getSourceDN() + 
                ", sType=" + event.getSourceType());
        } 

        // process object change
        processSDKNotification();
    }

    /**
     * Processes SDK notification for an entry, this method will go ahead to
     * remove the corresponding config entry affected by the change. So when
     * a new request comes, it will get the upto date authentication
     * configuration.
     */
    private void processSDKNotification() {
        try {
            if (debug.messageEnabled()) {
                debug.message("processSDKNotification name=" + configName);
            }
            ((AMConfiguration)Configuration.getConfiguration())
                .processListenerEvent(configName);
        } catch (Exception e) {
            debug.error("processSDKNotification", e);
        }
    }

    /**
     * Returns object to be listened.
     *
     * @return object to be listened.
     */
    public AMObject getListenedObject() {
        return amObject;
    }

    /**
     * Sets listened object.
     *
     * @param object Object to listen to.
     */
    public void setListenedObject(AMObject object) {
        amObject = object;
    }

}
