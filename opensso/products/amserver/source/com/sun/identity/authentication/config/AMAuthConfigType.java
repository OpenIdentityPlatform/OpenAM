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
 * $Id: AMAuthConfigType.java,v 1.3 2008/06/25 05:41:51 qcheng Exp $
 *
 */

package com.sun.identity.authentication.config;

import com.sun.identity.shared.debug.Debug;

/**
 * Represents an Authentication Configuration type.
 */
public class AMAuthConfigType {

    private static Debug debug = Debug.getInstance("amAuthConfig");

    protected static final int USER = 1;
    protected static final int ORGANIZATION = 2;
    protected static final int ROLE = 3;
    protected static final int SERVICE = 4;
    protected static final int MODULE = 5;

    private String configName;
    private String orgDN;
    private String indexName;
    private String clientType;
    private int indexType = 0;

    /** 
     * Constructor 
     */ 
    AMAuthConfigType (String name) { 
        // parse the configuration name
        if (name.startsWith(AMAuthConfigUtils.MODULE_KEY)) {
            // index point for organization dn
            int orgP = name.indexOf(";" + AMAuthConfigUtils.ORG_KEY + "=");
            // index point for client type
            int cliP = name.indexOf(";" + AMAuthConfigUtils.CLIENT_KEY + "=");
            if (orgP == -1 || cliP == -1) {
                // invalid config name
                debug.error("Invalid module config name " + name);
                return;
            }
            // valid module index
            indexType = MODULE;
            indexName = name.substring(
                AMAuthConfigUtils.MODULE_KEY.length() + 1, orgP);
            orgDN = name.substring(
                orgP + AMAuthConfigUtils.ORG_KEY.length() + 2, cliP);
            clientType = name.substring(
                cliP + AMAuthConfigUtils.CLIENT_KEY.length() + 2);
        } else if (name.startsWith(AMAuthConfigUtils.USER_KEY)) {
            // index point for organization dn
            int orgP = name.indexOf(";" + AMAuthConfigUtils.ORG_KEY + "=");
            // index point for client type
            int cliP = name.indexOf(";" + AMAuthConfigUtils.CLIENT_KEY + "=");
            if (orgP == -1 || cliP == -1) {
                // invalid config name
                debug.error("Invalid module config name " + name);
                return;
            }
            // valid user index
            indexType = USER;
            indexName = name.substring(
                AMAuthConfigUtils.USER_KEY.length() + 1, orgP);
            orgDN = name.substring(
                orgP + AMAuthConfigUtils.ORG_KEY.length() + 2, cliP);
            clientType = name.substring(
                cliP + AMAuthConfigUtils.CLIENT_KEY.length() + 2);
        } else if (name.startsWith(AMAuthConfigUtils.ORG_KEY)) {
            // index point for client type
            int cliP = name.indexOf(";" + AMAuthConfigUtils.CLIENT_KEY + "=");
            if (cliP == -1) {
                // invalid config name
                debug.error("Invalid module config name " + name);
                return;
            }
            // valid organization index
            indexType = ORGANIZATION;
            indexName = name.substring(
                AMAuthConfigUtils.ORG_KEY.length() + 1, cliP);
            orgDN = indexName;
            clientType = name.substring(
                cliP + AMAuthConfigUtils.CLIENT_KEY.length() + 2);
        } else if (name.startsWith(AMAuthConfigUtils.SERVICE_KEY)) {
            // index point for organization dn
            int orgP = name.indexOf(";" + AMAuthConfigUtils.ORG_KEY + "=");
            // index point for client type
            int cliP = name.indexOf(";" + AMAuthConfigUtils.CLIENT_KEY + "=");
            if (orgP == -1 || cliP == -1) {
                // invalid config name
                debug.error("Invalid module config name " + name);
                return;
            }
            // valid service index
            indexType = SERVICE;
            indexName = name.substring(
                AMAuthConfigUtils.SERVICE_KEY.length() + 1, orgP);
            orgDN = name.substring(
                orgP + AMAuthConfigUtils.ORG_KEY.length() + 2, cliP);
            clientType = name.substring(
                cliP + AMAuthConfigUtils.CLIENT_KEY.length() + 2);
        } else if (name.startsWith(AMAuthConfigUtils.ROLE_KEY)) {
            // index point for organization dn
            int orgP = name.indexOf(";" + AMAuthConfigUtils.ORG_KEY + "=");
            // index point for client type
            int cliP = name.indexOf(";" + AMAuthConfigUtils.CLIENT_KEY + "=");
            if (orgP == -1 || cliP == -1) {
                // invalid config name
                debug.error("Invalid module config name " + name);
                return;
            }
            // valid role index
            indexType = ROLE;
            indexName = name.substring(
                AMAuthConfigUtils.ROLE_KEY.length() + 1, orgP);
            orgDN = name.substring(
                orgP + AMAuthConfigUtils.ORG_KEY.length() + 2, cliP);
            clientType = name.substring(
                cliP + AMAuthConfigUtils.CLIENT_KEY.length() + 2);
        } else { 
            // invalid index type
            debug.message("Invalid index type in config name " + name);
        }

        if (debug.messageEnabled()) {
            debug.message("indexType = " + indexType +
               "\nindexName=" + indexName +
               "\norgDN=" + orgDN +
               "\nclientType=" + clientType);
        } 
    }

    /**
     * return organization DN
     */
    String getOrganization() {
        return orgDN;
    }

    /**
     * return client type 
     */
    String getClientType() {
        return clientType;
    }
    
    /**
     * return index name
     */
    String getIndexName() {
        return indexName;
    }
    
    /**
     * return index type 
     */
    int getIndexType() {
        return indexType;
    }
}
