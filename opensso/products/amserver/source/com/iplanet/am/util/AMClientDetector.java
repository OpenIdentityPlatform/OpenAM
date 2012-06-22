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
 * $Id: AMClientDetector.java,v 1.8 2008/09/04 16:16:34 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */
package com.iplanet.am.util;

import com.iplanet.services.cdm.ClientDetectionInterface;
import com.iplanet.services.cdm.ClientTypesManager;
import com.iplanet.services.cdm.DefaultClientTypesManager;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * This is an utility to get the client type. This utility executes Client
 * Detection Class provided in Client Detection Module Service and gets the
 * client type. Default client type will be returned if there is no Client
 * Detection Implementation provided
 * @supported.all.api
 */
public class AMClientDetector {

    private static ClientTypesManager clientManager;

    private static Map attrs;

    private static boolean detectionEnabled;

    private ClientDetectionInterface clientDetector;

    private static ClientDetectionInterface defaultClientDetector;

    private static final String CDM_SERVICE_NAME = "iPlanetAMClientDetection";

    private static final String CDM_CLASS_NAME = 
        "iplanet-am-client-detection-class";

    private static final String CLIENT_TYPES_MANAGER_CLASS_NAME = 
        "iplanet-am-client-types-manager-class";

    private static final String CDM_ENABLED_ATTR = 
        "iplanet-am-client-detection-enabled";

    private static Debug debug = Debug.getInstance("amClientDetection");

    private static String ClientTypesManagerImpl = null;

    private static String clientDetectionClass = null;

    private static boolean servicePassed = true;

    static {
        getServiceSchemaManager();
        if (servicePassed) {
            getClientAttributes();
            if (detectionEnabled) {
                initClientTypesManager();
                executeClientDetector();
            }
        }
    }

    /**
     * Constructs a <code>AMClientDetector</code> instance.
     */
    public AMClientDetector() {
        clientDetector = defaultClientDetector;
    }

    /**
     * Applications can provide client detector implementation class
     * 
     * @param className
     *            the name of the implementation class
     */
    public AMClientDetector(String className) {
        if (className != null) {
            try {
                clientDetector = (ClientDetectionInterface) (Class
                        .forName(className).newInstance());
            } catch (Exception ex) {
                clientDetector = defaultClientDetector;
            }
        } else {
            clientDetector = defaultClientDetector;
        }
    }

    /**
     * Application provide custom detection class.
     * 
     * @param cd
     *            class that implements <code>ClientDetectionInterface</code>.
     */
    public AMClientDetector(ClientDetectionInterface cd) {
        clientDetector = cd;
    }

    /**
     * Returns the client type by executing client
     * detector class provided in Client Detection Service.
     * 
     * @param request
     *            HTTP Servlet Request.
     * @return client type . Default client type will be returned if either the
     *         client detection is not enabled or the client detector class is
     *         not provided in <code>cdm</code> service. If it throws any
     *         exception by executing this class, this method will just return
     *         null .
     */
    public String getClientType(HttpServletRequest request) {
        String clientType = null;
        debug.message("AMClientDetector.getClientType()");
        // Check whether the client detection is enabled or not
        if (detectionEnabled) {
            try {
                if (clientDetector != null) {
                    clientType = clientDetector.getClientType(request);
                }
                if (debug.messageEnabled()) {
                    debug.message("AMClientDetector: Client Type : "
                                    + clientType);
                }                
            } catch (Exception ex) {
                clientType = clientManager.getDefaultClientType();
            }
        }        
        
        if (clientType == null || clientType.length() == 0)
            clientType = "genericHTML";
        
        if (debug.messageEnabled()) {
            debug.message("AMClientDetector: Default Client Type : " 
                + clientType);
        }
        return clientType;
    }

    /**
     * Returns <code>true</code> if the client detection is enabled.
     * 
     * @return <code>true</code> if the client detection is enabled.
     */
    public boolean isDetectionEnabled() {
        return detectionEnabled;
    }

    /**
     * Returns the <code>ClientTypesManager</code> Instance.
     * 
     * @return the <code>ClientTypesManager</code> Instance.
     */
    public static ClientTypesManager getClientTypesManagerInstance() {
        return clientManager;
    }

    /* create instance of ClientTypesManager */

    private static void initClientTypesManager() {

        if ((ClientTypesManagerImpl != null)
                && (ClientTypesManagerImpl.length() > 0)) {

            try {
                clientManager = (ClientTypesManager) Class.forName(
                        ClientTypesManagerImpl).newInstance();
            } catch (Exception ex) {
                debug.error("Unable to instantiate class ", ex);
                clientManager = new DefaultClientTypesManager();
            }
        } else {
            clientManager = new DefaultClientTypesManager();
        }
        clientManager.initManager();
        Set allClientTypes = clientManager.getAllClientTypes();
        if (allClientTypes == null || allClientTypes.isEmpty()) {
            if (debug.warningEnabled()) {
                debug.warning("AMClientDetector.initClientManager():"
                        + " no client types found, "
                        + " setting detectionEnabled to false");
            }
            detectionEnabled = false;
        }

        return;
    }

    /* retrieve client attributes from the ClientDetection service */
    private static void getServiceSchemaManager() {
        try {
            SSOToken adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                    CDM_SERVICE_NAME, adminToken);

            ServiceSchema gsc = ssm.getGlobalSchema();

            attrs = gsc.getAttributeDefaults();
        } catch (Exception e) {
            debug.error("AMClientDetector.static: ", e);
            servicePassed = false;
            return;
        }
    }

    private static void getClientAttributes() {
        if (attrs != null) {
            String det = Misc.getMapAttr(attrs, CDM_ENABLED_ATTR);
            if (det != null && det.equalsIgnoreCase("true")) {
                detectionEnabled = true;
            } else {
                detectionEnabled = false;
            }
            if (debug.messageEnabled()) {
                debug.message("AMClientDetector: ClientDetection enable : "
                        + detectionEnabled);
            }
            clientDetectionClass = Misc.getMapAttr(attrs, CDM_CLASS_NAME);

            ClientTypesManagerImpl = Misc.getMapAttr(attrs,
                    CLIENT_TYPES_MANAGER_CLASS_NAME);
        }
    }

    private static void executeClientDetector() {

        if ((clientDetectionClass != null)
                && (clientDetectionClass.length() != 0)) {
            try {
                defaultClientDetector = (ClientDetectionInterface) (Class
                        .forName(clientDetectionClass).newInstance());
            } catch (ClassNotFoundException ex) {
                debug.warning("AMClientDetector.executeClientDetector():"
                        + " ClassNotFound: " + ex.getMessage());
            } catch (InstantiationException ex) {
                debug.warning("AMClientDetector.executeClientDetector():"
                        + " not able to instantiate: " + clientDetectionClass);
            } catch (IllegalAccessException ex) {
                debug.warning("AMClientDetector.executeClientDetector():"
                        + " IllegalAccessException: " + ex.getMessage());
            }
        }
    }

    /**
     * Returns true if the client detection service is present or false if the
     * client detection service is not present and the client attributes could
     * not be retrieved.
     * 
     * @return false if no Service is present. The default value is true.
     */
    public static boolean isServicePassed() {
        return servicePassed;
    }
}
