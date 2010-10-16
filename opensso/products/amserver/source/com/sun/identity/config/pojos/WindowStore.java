/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WindowStore.java,v 1.4 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author Jeffrey Bermudez
 */
public class WindowStore extends AuthenticationStore {

    private String kerberosServicePrincipal;
    private String kerberosFileName;
    private String kerberosRealm;
    private String kerberosServiceName;
    private String domainName;


    public String getKerberosServicePrincipal() {
        return kerberosServicePrincipal;
    }

    public void setKerberosServicePrincipal(String kerberosServicePrincipal) {
        this.kerberosServicePrincipal = kerberosServicePrincipal;
    }

    public String getKerberosFileName() {
        return kerberosFileName;
    }

    public void setKerberosFileName(String kerberosFileName) {
        this.kerberosFileName = kerberosFileName;
    }

    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public String getKerberosServiceName() {
        return kerberosServiceName;
    }

    public void setKerberosServiceName(String kerberosServiceName) {
        this.kerberosServiceName = kerberosServiceName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
}
