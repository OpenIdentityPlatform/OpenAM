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

package org.forgerock.openam.session.model;

import java.io.Serializable;

/**
 * OpenAM Session DB OpenDJ Server POJO
 *
 * @author steve
 */
public class AMSessionDBOpenDJServer implements Serializable {
    private static final long serialVersionUID = 1L;
    private String hostName;
    private String adminPort;
    private String ldapPort;
    private String jmxPort;
    private String replPort;
    
    public AMSessionDBOpenDJServer(String hostName, 
                                   String adminPort, 
                                   String ldapPort, 
                                   String jmxPort,
                                   String replPort) {
        this.hostName = hostName;
        this.adminPort = adminPort;
        this.ldapPort = ldapPort;
        this.jmxPort = jmxPort;
        this.replPort = replPort;
    }
    
    public AMSessionDBOpenDJServer() {
        // do nothing
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
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
    
    public String getReplPort() {
        return replPort;
    }
    
    public void setReplPort(String replPort) {
        this.replPort = replPort;
    }


    public enum AmSessionDbAttr {
        ADMIN_PORT("adminPort"), LDAP_PORT("ldapPort"), JMX_PORT("jmxPort"), REPL_PORT("replPort");

        private final String text;

        private AmSessionDbAttr(String text) {
            this.text = text;
        }

        @Override public String toString() {
            return text;
        }
    }


}
