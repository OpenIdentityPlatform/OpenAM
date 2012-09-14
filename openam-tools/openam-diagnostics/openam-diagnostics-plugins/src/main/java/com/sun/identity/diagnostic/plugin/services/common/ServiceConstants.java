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
 * $Id: ServiceConstants.java,v 1.2 2008/12/16 17:54:07 ak138937 Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.diagnostic.plugin.services.common;

import com.sun.identity.shared.Constants;

/**
 * This interface contains the property names used by the 
 * services
 */
public interface ServiceConstants extends Constants {
    /*
     * Property strings for bootrap file parsing.
     */
    String PROTOCOL = "protocol";
    String SERVER_INSTANCE = "serverinstance";
    String FF_BASE_DIR = "ffbasedir";
    String BASE_DIR = "basedir";
    String DS_HOST = "dshost";
    String DS_PORT = "dsport";
    String DS_PWD = "dspwd";
    String DS_MGR = "dsmgr";
    String DS_BASE_DN = "dsbasedn";
    String PWD = "pwd";
    String EMBEDDED_DS = "embeddedds";
    String PROT_FILE = "file";
    String PROT_LDAP = "ldap";
    String PROTOCOL_FILE = "file://";
    String PROTOCOL_LDAP = "ldap://";
    String PROTOCOL_LDAPS = "ldaps://";
    String DS_PROTOCOL = "dsprotocol";
    String LOCATION = "location";
    String BOOTSTRAP = "bootstrap";
    String BOOTPATH = "path";
    String BASE_DIR_PATTERN = "%BASE_DIR%";
    String DEP_URI_PATTERN = "%SERVER_URI%";
    
    /**
     * Session failover related properties to be checked in Server 
     * configuration service
     */
    final String amSessionService = "iPlanetAMSessionService";
    final String IS_SFO_ENABLED = "iplanet-am-session-sfo-enabled";
    final String SESSION_STORE_USERNAME =
        "iplanet-am-session-store-username";
    final String SESSION_STORE_PASSWORD =
        "iplanet-am-session-store-password";
    final String CONNECT_MAX_WAIT_TIME =
        "iplanet-am-session-store-cpl-max-wait-time";
    final String JDBC_DRIVER_CLASS =
        "iplanet-am-session-JDBC-driver-Impl-classname";
    static final String IPLANET_AM_SESSION_REPOSITORY_URL =
         "iplanet-am-session-repository-url";
    static final String MIN_POOL_SIZE =
        "iplanet-am-session-min-pool-size";
    final String MAX_POOL_SIZE =
        "iplanet-am-session-max-pool-size";
    static final String IS_SFO_ENABLED =
            "iplanet-am-session-sfo-enabled";

    /**
     * General Server  related properties
     */
    final String CONFIG_PATH = "com.iplanet.services.configpath";

    /**
     * Single line format string 
     */
     final String SMALL_LINE_SEP_1 = "---------------------------------------";

    /**
     * Double line format string 
     */
     final String PARA_SEP_1 = "==============================";
}
