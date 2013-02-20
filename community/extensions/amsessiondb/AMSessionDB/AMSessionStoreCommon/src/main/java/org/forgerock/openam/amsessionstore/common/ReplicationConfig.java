/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.common;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author steve
 */
public class ReplicationConfig implements Serializable {
    private static final long serialVersionUID = 1L;
        
    private String adminPort = null;
    private String ldapPort = null;
    private String jmxPort = null;
    private String dsMgrDN = null;
    private String dsMgrPasswd = null;
    private String replPort = null;
    private String odjRoot = null;
    private String sessionDBSuffix = null;
    private String existingServerUrl = null;
    private String hostUrl = null;
    
    public ReplicationConfig(Map<String, String> config) {
        adminPort = config.get(Constants.OPENDJ_ADMIN_PORT);
        ldapPort = config.get(Constants.OPENDJ_LDAP_PORT);
        jmxPort = config.get(Constants.OPENDJ_JMX_PORT);
        dsMgrDN = config.get(Constants.OPENDJ_DS_MGR_DN);
        dsMgrPasswd = config.get(Constants.OPENDJ_DS_MGR_PASSWD);
        replPort = config.get(Constants.OPENDJ_REPL_PORT);
        odjRoot = config.get(Constants.OPENDJ_ROOT);
        sessionDBSuffix = config.get(Constants.OPENDJ_SUFFIX);
        existingServerUrl = config.get(Constants.EXISTING_SERVER_URL);
        hostUrl = config.get(Constants.HOST_URL);
    }   
    
    public ReplicationConfig() {
        // do nothing
    }
    
    public String getExistingServerUrl() {
        return existingServerUrl;
    }
    
    public void setExistingServerUrl(String existingServerUrl) {
        this.existingServerUrl = existingServerUrl;
    }
    
    public String getOdjRoot() {
        return odjRoot;
    }
    
    public void setOdjRoot(String odjRoot) {
        this.odjRoot = odjRoot;
    }
    
    public String getSessionDBSuffix() {
        return sessionDBSuffix;
    }
    
    public void setSessionDBSuffix(String sessionDBSuffix) {
        this.sessionDBSuffix = sessionDBSuffix;
    }
    
    public String getAdminPort() {
        return adminPort;
    }
    
    public void setAdminPort(String adminPort) {
        this.adminPort = adminPort;
    }
    
    public String getLdapPort() {
        return ldapPort;
    }
    
    public void setLdapPort(String ldapPort) {
        this.ldapPort = ldapPort;
    }
    
    public String getJmxPort() {
        return jmxPort;
    }
    
    public void setJmxPort(String jmxPort) {
        this.jmxPort = jmxPort;
    }
    
    public String getDsMgrDN() {
        return dsMgrDN;
    }
    
    public void setDsMgrDN(String dsMgrDN) {
        this.dsMgrDN = dsMgrDN;
    }
    
    public String getDsMgrPasswd() {
        return dsMgrPasswd;
    }
    
    public void setDsMgrPasswd(String dsMgrPasswd) {
        this.dsMgrPasswd = dsMgrPasswd;
    }
    
    public String getReplPort() {
        return replPort;
    }
    
    public void setReplPort(String replPort) {
        this.replPort = replPort;
    }
    
    public String getHostUrl() {
        return hostUrl;
    }
    
    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }
}

