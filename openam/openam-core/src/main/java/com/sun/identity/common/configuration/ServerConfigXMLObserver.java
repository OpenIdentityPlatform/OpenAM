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
 * $Id: ServerConfigXMLObserver.java,v 1.2 2009/11/04 19:25:35 veiming Exp $
 */

package com.sun.identity.common.configuration;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.BootstrapCreator;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;

/**
 * Listens to directory configuration changes in server configuration node.
 * @author veiming
 */
public class ServerConfigXMLObserver implements ConfigurationListener {
    private static ServerConfigXMLObserver instance;
    private static Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
    private static String currentXML;

    static {
        instance = new ServerConfigXMLObserver();
        ConfigurationObserver.getInstance().addListener(instance);
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            currentXML = ServerConfiguration.getServerConfigXML(adminToken, 
                SystemProperties.getServerInstanceName());
        } catch (SMSException e) {
            debug.error("ServerConfigXMLObserver.init", e);
        } catch (SSOException e) {
            debug.error("ServerConfigXMLObserver.init", e);
        }
    }

    private ServerConfigXMLObserver() {
    }

    /**
     * Returns an instance of <code>ServerConfigXMLObserver</code> object.
     *
     * @return an instance of <code>ServerConfigXMLObserver</code> object.
     */
    public static ServerConfigXMLObserver getInstance() {
        return instance;
    }

    /**
     * This method will be call if configuration changed.
     */    
    public void notifyChanges() {
        update(false);
    }

    /**
     * Updates bootstrap file.
     * 
     * @param bForce <code>true</code> to update regardless if the
     *        tracking XML string in this instance is the same as 
     *        the value in server configuration node.
     */
    public void update(boolean bForce) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            String xml = ServerConfiguration.getServerConfigXML(adminToken,
                SystemProperties.getServerInstanceName());

             /*
              * xml can be null for the following reasons
              * 1. old DIT
              * 2. new DIT but the serverconfig.xml is not stored in centralized
              *    configuration
              */
            if ((xml != null) && (xml.trim().length() > 0)){ 
                if (bForce) {
                    currentXML = null;
                }
                if ((currentXML == null) || !currentXML.equals(xml)) {
                    BootstrapCreator.updateBootstrap();
                    currentXML = xml;
                }
            }
        } catch (ConfigurationException e) {
            debug.error("ServerConfigXMLObserver.notifyChanges", e);
        } catch (SMSException e) {
            debug.error("ServerConfigXMLObserver.notifyChanges", e);
        } catch (SSOException e) {
            debug.error("ServerConfigXMLObserver.notifyChanges", e);
        }
    }
}

