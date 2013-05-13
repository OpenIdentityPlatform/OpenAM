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
 * $Id: AMLDAPCertStoreParameters.java,v 1.3 2009/01/28 05:35:12 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock Inc
 */

package com.sun.identity.security.cert;

import com.sun.identity.shared.ldap.LDAPSocketFactory;

/**
 * Parameters used to access ldap cert store
 */
public class AMLDAPCertStoreParameters {
    private String serverName = null;
    private int serverPort;
    private String ldapUser = null;
    private String password = null;
    private String searchFilter = null;
    private String startSearchLoc = null;  
    private boolean secureLdap = false;
    private String uriParams = null;
    private LDAPSocketFactory sockFactory = null;
    static public final int ldap_version = 3;
    private boolean doCRLCaching = true;

    /**
     * @param server
     * @param port
     */
    public AMLDAPCertStoreParameters(String server, int port) {
        serverName = server;
        serverPort = port;
    }

    public static AMLDAPCertStoreParameters
        setLdapStoreParam(AMLDAPCertStoreParameters ldapParams,
                          String user, String passwd, String searchLoc,
                          String uriParamsCRL, boolean secureCon) {
            ldapParams.setUser(user);
            ldapParams.setPassword(passwd);
            ldapParams.setStartLoc(searchLoc);
            ldapParams.setURIParams(uriParamsCRL);
            ldapParams.setSecure(secureCon);
            
            return ldapParams;
    }

    /**
     * Get host name 
     */
    public String getServerName() {
        return  serverName;
    }

    /**
     * Get port number 
     */
    public int getPort() {
        return serverPort;  
    }

    /**
     * Set ldap search start loc  
     */
    public void setStartLoc(String startloc) {
        startSearchLoc = startloc;  
    }

    /**
     * Get ldap search start loc  
     */
    public String getStartLoc() {
        return startSearchLoc;  
    }

    /**
     * Set ldap user for bind  
     */
    public void setUser(String uid) {
        ldapUser = uid;
    }

    /**
     * Get ldap user for bind  
     */
    public String getUser() {
        return ldapUser;
    }

    /**
     * Set ldap password for bind  
     */
    public void setPassword(String passwd) {
        password = passwd;
    }

    /**
     * Get ldap password for bind  
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set URI params for CRL search
     */
    public void setURIParams(String params) {
        uriParams = params;
    }

    /**
     * Get URI params for CRL search
     */
    public String getURIParams() {
        return uriParams;
    }

    /**
     * Set search filter for ldap search
     */
    public void setSearchFilter(String filter) {
        searchFilter = filter;
    }

    /**
     * Get search filter for ldap search
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    /**
     * Set ldap port is secure
     */
    public void setSecure(boolean secure) {
        secureLdap = secure;
    }

    /**
     * Get ldap port is secure
     */
    public boolean isSecure() {
        return secureLdap;
    }

    /**
     * Set secure socket factory  
     */
    public void setSecureSocketFactory(LDAPSocketFactory sf) {
        sockFactory = sf;
    }

    /**
     * Get secure socket factory  
     */
    public LDAPSocketFactory getSecureSocketFactory() {
        return sockFactory;
    }

    /**
     * @return the doCRLCaching
     */
    public boolean isDoCRLCaching() {
        return doCRLCaching;
    }

    /**
     * @param doCRLCaching the doCRLCaching to set
     */
    public void setDoCRLCaching(boolean doCRLCaching) {
        this.doCRLCaching = doCRLCaching;
    }
    
    
}
