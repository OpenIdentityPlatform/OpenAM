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
 * $Id: ConfigConstants.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.server;

import com.sun.identity.diagnostic.plugin.services.common.ServiceConstants;


/**
 * This interface contains the constants used by the 
 * Server configuration service 
 */
public interface ConfigConstants extends ServiceConstants {

    /**
     * Resource file name used by Server configuration service
     */
    String SVR_CFG_RESOURCE_BUNDLE = "ServerConfigService";

    /**
     * Valid properties file name used by Server configuration service
     */
    final String VALID_SERVER_PROP = "validserverconfig";

    /**
     * Deafult properties file name used by Server configuration service
     */
    final String DEFAULT_SERVER_PROP = "serverdefaults";

    /**
     * SAML related properties to be checked in Server configuration service
     */
    final String XML_SIG_ALG_PROP = 
        "com.sun.identity.saml.xmlsig.xmlSigAlgorithm";
    final String XML_SIG_ALG_VAL = "http://www.w3.org/2000/09/xmldsig";
    
    /**
     * Persistent search property
     */
    final String DISABLE_PERSISTENT_SEARCH=
         "com.sun.am.event.connection.disable.list";
}
