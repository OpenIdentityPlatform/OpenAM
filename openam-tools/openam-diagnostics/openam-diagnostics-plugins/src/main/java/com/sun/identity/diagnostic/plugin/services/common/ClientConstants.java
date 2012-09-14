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
 * $Id: ClientConstants.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.common;

/**
 * This interface contains the property names used by the 
 * client services
 */
public interface ClientConstants extends ServiceConstants {

   /**
    * Generic constants used by Agent service
    */
    String AGENT_DETAIL_ATTR = "identitydetails.attribute=";
    String AGENT_DETAIL_ATTR_NAME = "identitydetails.attribute.name";
    String AGENT_DETAIL_ATTR_VALUE = "identitydetails.attribute.value";
    String AGENT_BOOTSTRAP_FILE = "OpenSSOAgentBootstrap.properties";
    String AGENT_SERVICE_TAG = "AM_SERVICES_";

    /**
     * J2EE agent related properties
     */
    String J2EE_AGENT_USER_NAME = "com.sun.identity.agents.app.username";

    /**
     * Web agent related properties
     */
    String WEB_AGENT_USER_NAME = "com.sun.identity.agents.config.username";
    String WEB_AGENT_SECRET = "com.sun.identity.agents.config.password";
    String WEB_AGENT_KEY = "com.sun.identity.agents.config.key";
    String WEB_AGENT_NAMING_URL = "com.sun.identity.agents.config.naming.url";
}
