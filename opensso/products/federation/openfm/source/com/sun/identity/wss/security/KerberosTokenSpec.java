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
 * $Id: KerberosTokenSpec.java,v 1.3 2008/08/27 19:05:52 mrudul_uchil Exp $
 *
 */
package com.sun.identity.wss.security;

import java.util.Map;


/**
 * This class represents Kerberos Security Token Specification.
 * It implements <code>SecurityTokenSpec</code> interface.
 * 
 * @supported.all.api
 */
public class KerberosTokenSpec implements SecurityTokenSpec {
    
    private String encodedString = null;
    private String servicePrincipal = null;
    private String kdcServer = null;
    private String kdcDomain = null;
    private String ticketCacheDir = null;
    private String keytab = null;
    
    /** Creates a new instance of KerberosToken */
    public KerberosTokenSpec() {       
        
    }    
    
    public String getValueType() {       
       return WSSConstants.KERBEROS_VALUE_TYPE;
    }
    
    public String getEncodingType() {
        return BinarySecurityToken.BASE64BINARY;
    }
    
    public String getServicePrincipal() {
        return servicePrincipal;
    }
    
    public void setServicePrincipal(String principal) {
        this.servicePrincipal = principal;
    }
    
    public String getKDCServer() {
        return kdcServer;
    }
    
    public void setKDCServer(String kdcServer) {
        this.kdcServer = kdcServer;
    }
    
    public String getKDCDomain() {
        return kdcDomain;
    }
    
    public void setKDCDomain(String kdcDomain) {
        this.kdcDomain = kdcDomain;
    }
        
    public String getKeytabFile() {
        return keytab;
    }
    
    public void setKeytabFile(String file) {
        this.keytab = file;
    }
    
    public String getTicketCacheDir() {
        return ticketCacheDir;
    }
    
    public void setTicketCacheDir(String cacheDir) {
        this.ticketCacheDir = cacheDir;
    }
    
}
