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
 * $Id: ConnectionConstants.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.connect;

import com.sun.identity.shared.Constants;

/**
 * This interface contains the property names used by the 
 * Server connection service 
 */
public interface ConnectionConstants extends Constants {
    /**
     * Resource file name used by Connection service
     */
    final String CONNECT_RESOURCE_BUNDLE = "ServerConnection";
    
    /**
     * Constants for retrieving directory information
     */
    final String LDAPv3Config_AUTHPW =
        "sun-idrepo-ldapv3-config-authpw";
    final String LDAPv3Config_AUTHID =
        "sun-idrepo-ldapv3-config-authid";
    final String LDAPv3Config_LDAP_SSL_ENABLED =
        "sun-idrepo-ldapv3-config-ssl-enable";
    final String LDAPv3Config_LDAP_SERVER =
        "sun-idrepo-ldapv3-config-ldap-server";
    final String IDREPO_SERVICE = "sunIdentityRepositoryService";
}
